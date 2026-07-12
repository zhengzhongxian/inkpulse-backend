package com.inkpulse.features.order.handlers;

import com.inkpulse.constants.QueueConstants;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.BookEdition;
import com.inkpulse.entities.Order;
import com.inkpulse.entities.OrderDetail;
import com.inkpulse.entities.OrderLog;
import com.inkpulse.entities.RefundRequest;
import com.inkpulse.entities.enums.OrderStatus;
import com.inkpulse.entities.enums.PaymentMethod;
import com.inkpulse.entities.enums.PaymentStatus;
import com.inkpulse.entities.enums.RefundStatus;
import com.inkpulse.features.order.commands.ReturnOrderCommand;
import com.inkpulse.features.order.rules.ReturnOrderContext;
import com.inkpulse.features.order.dto.ReturnGhnOrderMessage;
import com.inkpulse.pipeline.EligibilityContext;
import com.inkpulse.pipeline.EligibilityPipeline;
import com.inkpulse.pipeline.IEligibilityRule;
import com.inkpulse.repositories.BookEditionRepository;
import com.inkpulse.repositories.OrderDetailRepository;
import com.inkpulse.repositories.OrderLogRepository;
import com.inkpulse.repositories.OrderRepository;
import com.inkpulse.repositories.RefundRequestRepository;
import com.inkpulse.service.outbox.OutboxPublisher;
import com.inkpulse.constants.message.OrderMessageConstants;
import com.inkpulse.entities.OrderEvent;
import com.inkpulse.entities.StockTransaction;
import com.inkpulse.entities.enums.OrderEventType;
import com.inkpulse.entities.enums.StockTransactionType;
import com.inkpulse.repositories.OrderEventRepository;
import com.inkpulse.repositories.StockTransactionRepository;
import com.inkpulse.features.book.dto.BookEditionSyncHelper;
import com.inkpulse.features.book.dto.SyncBookEditionMessage;
import com.inkpulse.cache.ICacheService;
import com.inkpulse.cache.CacheProperties;
import com.inkpulse.constants.KeyConstants;
import com.inkpulse.corehelpers.JsonHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReturnOrderHandler implements Command.CommandHandler<ReturnOrderCommand, Void> {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final OrderLogRepository orderLogRepository;
    private final BookEditionRepository bookEditionRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final OutboxPublisher outboxPublisher;

    private final OrderEventRepository orderEventRepository;
    private final StockTransactionRepository stockTransactionRepository;
    private final BookEditionSyncHelper bookEditionSyncHelper;
    private final ICacheService cacheService;
    private final CacheProperties cacheProperties;

    private final List<IEligibilityRule<ReturnOrderContext>> returnRules;

    @Override
    @Transactional
    public Void handle(ReturnOrderCommand command) {
        log.info("Handling ReturnOrderCommand for order code: {}, by admin: {}", 
                command.getOrderCode(), command.getAdminUserId());

        // 1. Run eligibility rules
        ReturnOrderContext returnCtx = new ReturnOrderContext(command);
        EligibilityPipeline<ReturnOrderContext> pipeline = new EligibilityPipeline<>(returnRules);
        EligibilityContext<ReturnOrderContext> context = pipeline.run(returnCtx);

        if (context.isRejected()) {
            log.warn("Return order validation rejected: {}", context.getRejectionReason());
            throw new BusinessValidationException(context.getRejectionReason(), "VALIDATION_FAILED");
        }

        Order order = returnCtx.getOrder();
        OrderStatus fromStatus = order.getOrderStatus();

        // 2. Restock Book Editions (Atomic SQL + Stock Event + ES/Redis Sync)
        List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getId());
        for (OrderDetail detail : details) {
            BookEdition edition = detail.getBookEdition();
            bookEditionRepository.incrementStock(edition.getId(), detail.getQuantity());

            // Update local object to sync with ELS correctly
            edition.setStockQuantity(edition.getStockQuantity() + detail.getQuantity());

            // Save Stock Transaction
            StockTransaction tx = StockTransaction.builder()
                    .edition(edition)
                    .delta(detail.getQuantity())
                    .type(StockTransactionType.RETURN_RESTORE)
                    .referenceCode(order.getOrderCode())
                    .note(OrderMessageConstants.RETURNED_ADMIN_NOTE)
                    .createdBy(UUID.fromString(command.getAdminUserId()))
                    .build();
            stockTransactionRepository.save(tx);

            // Sync with Elasticsearch
            SyncBookEditionMessage syncMsg = bookEditionSyncHelper.buildSyncMessage(edition);
            if (syncMsg != null) {
                outboxPublisher.publish(
                        QueueConstants.SYNC_BOOK_EDITION_PARTIAL,
                        syncMsg,
                        "urn:message:InkPulse.Worker.Features.Book.Messages:SyncBookEditionMessage"
                );
            }

            // Evict Cache
            try {
                String cacheKey = cacheProperties.buildKey(KeyConstants.SECTION_BOOK_EDITION_DETAIL, edition.getId().toString());
                cacheService.remove(cacheKey);
            } catch (Exception ex) {
                log.error("Failed to evict detail cache for edition: {}", edition.getId(), ex);
            }

            log.info("Restocked book edition {} by +{} due to return request (ISBN: {})", 
                    edition.getId(), detail.getQuantity(), edition.getIsbn());
        }

        // 3. Update Order Status to CANCELLED
        order.setOrderStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        // Save Order Event (Order Event Store)
        OrderEvent orderEvent = OrderEvent.builder()
                .order(order)
                .eventType(OrderEventType.ORDER_RETURNED)
                .eventData(JsonHelper.serializeSafe(command))
                .createdBy(UUID.fromString(command.getAdminUserId()))
                .build();
        orderEventRepository.save(orderEvent);

        // 4. Create Order Log
        OrderLog orderLog = OrderLog.builder()
                .order(order)
                .fromStatus(fromStatus)
                .toStatus(OrderStatus.CANCELLED)
                .changedBy(UUID.fromString(command.getAdminUserId()))
                .adminNote(OrderMessageConstants.RETURNED_ADMIN_NOTE)
                .userNote(OrderMessageConstants.RETURNED_USER_NOTE)
                .build();
        orderLogRepository.save(orderLog);

        // 5. Publish ReturnGhnOrderMessage to Worker
        if (order.getGhnOrderCode() != null && !order.getGhnOrderCode().trim().isEmpty()) {
            ReturnGhnOrderMessage msg = ReturnGhnOrderMessage.builder()
                    .orderCode(order.getOrderCode())
                    .ghnOrderCode(order.getGhnOrderCode())
                    .build();

            outboxPublisher.publish(
                    QueueConstants.RETURN_GHN_ORDER,
                    msg,
                    "urn:message:InkPulse.Worker.Features.Order.Messages:ReturnGhnOrderMessage"
            );
            log.info("Published ReturnGhnOrderMessage to outbox for GHN order: {}", order.getGhnOrderCode());
        }

        // 6. If payment is PAID and method is PAYOS, automatically create Refund Request
        if (order.getPaymentMethod() == PaymentMethod.PAYOS && order.getPaymentStatus() == PaymentStatus.PAID) {
            RefundRequest refundRequest = RefundRequest.builder()
                    .order(order)
                    .amount(order.getOrderFee().add(order.getShippingFee()))
                    .status(RefundStatus.PENDING)
                    .reason("Yêu cầu hoàn trả đơn hàng đã thanh toán qua PayOS")
                    .build();
            refundRequestRepository.save(refundRequest);
            log.info("Created PENDING RefundRequest for returned order: {} (Amount: {})", 
                    order.getOrderCode(), refundRequest.getAmount());
        }

        log.info("Successfully requested return/cancelled order: {}", order.getOrderCode());
        return null;
    }
}
