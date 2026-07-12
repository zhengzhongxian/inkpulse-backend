package com.inkpulse.features.order.handlers;

import com.inkpulse.constants.QueueConstants;
import com.inkpulse.constants.message.OrderMessageConstants;
import com.inkpulse.cqrs.Command;
import com.inkpulse.crypto.ICryptographyService;
import com.inkpulse.entities.BookEdition;
import com.inkpulse.entities.Order;
import com.inkpulse.entities.OrderDetail;
import com.inkpulse.entities.enums.PaymentMethod;
import com.inkpulse.features.order.commands.ConfirmPackOrderCommand;
import com.inkpulse.models.response.order.ConfirmPackOrderResponse;
import com.inkpulse.features.order.rules.ConfirmPackOrderContext;
import com.inkpulse.features.order.dto.CreateGhnOrderMessage;
import com.inkpulse.pipeline.EligibilityContext;
import com.inkpulse.pipeline.EligibilityPipeline;
import com.inkpulse.pipeline.IEligibilityRule;
import com.inkpulse.repositories.OrderDetailRepository;
import com.inkpulse.repositories.OrderRepository;
import com.inkpulse.repositories.OrderEventRepository;
import com.inkpulse.entities.OrderEvent;
import com.inkpulse.entities.enums.OrderEventType;
import com.inkpulse.corehelpers.JsonHelper;
import com.inkpulse.service.outbox.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ConfirmPackOrderHandler implements Command.CommandHandler<ConfirmPackOrderCommand, ConfirmPackOrderResponse> {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ICryptographyService cryptographyService;
    private final OutboxPublisher outboxPublisher;
    private final OrderEventRepository orderEventRepository;
    private final List<IEligibilityRule<ConfirmPackOrderContext>> packRules;

    @Override
    @Transactional
    public ConfirmPackOrderResponse handle(ConfirmPackOrderCommand command) {
        log.info("Handling ConfirmPackOrderCommand for order code: {}, admin: {}", command.getOrderCode(), command.getAdminUserId());

        // 1. Run eligibility rules
        ConfirmPackOrderContext packCtx = new ConfirmPackOrderContext(command);
        EligibilityPipeline<ConfirmPackOrderContext> pipeline = new EligibilityPipeline<>(packRules);
        EligibilityContext<ConfirmPackOrderContext> context = pipeline.run(packCtx);

        if (context.isRejected()) {
            log.warn("Confirm pack order rejected: {}", context.getRejectionReason());
            throw new BusinessValidationException(context.getRejectionReason(), "VALIDATION_FAILED");
        }

        Order order = packCtx.getOrder();

        // 2. Fetch order items
        List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getId());
        List<CreateGhnOrderMessage.OrderItemInfo> itemInfos = new ArrayList<>();

        int totalWeight = 0;
        int totalHeight = 0;
        int maxWidth = 0;
        int maxLength = 0;

        for (OrderDetail detail : details) {
            BookEdition edition = detail.getBookEdition();
            itemInfos.add(CreateGhnOrderMessage.OrderItemInfo.builder()
                    .name(edition.getBook() != null ? edition.getBook().getTitle() : edition.getIsbn())
                    .code(edition.getIsbn())
                    .quantity(detail.getQuantity())
                    .price(detail.getOriginalPrice().intValue())
                    .weight(edition.getWeightGram())
                    .length(edition.getLengthCm())
                    .width(edition.getWidthCm())
                    .height(edition.getHeightCm())
                    .build());

            totalWeight += edition.getWeightGram() * detail.getQuantity();
            totalHeight += edition.getHeightCm() * detail.getQuantity();
            maxWidth = Math.max(maxWidth, edition.getWidthCm());
            maxLength = Math.max(maxLength, edition.getLengthCm());
        }

        int insuranceVal = Math.min(5000000, order.getOrderFee().intValue());
        int codAmount = 0;
        if (order.getPaymentMethod() == PaymentMethod.COD) {
            codAmount = order.getOrderFee().add(order.getShippingFee()).intValue();
        }

        String fullAddress = order.getStreetAddress() + ", " + 
                order.getWard().getWardName() + ", " + 
                order.getDistrict().getDistrictName() + ", " + 
                order.getProvince().getProvinceName();

        // Encrypt sensitive info for RabbitMQ message
        String encryptedPhone = cryptographyService.encrypt(order.getRecipientPhone());
        String encryptedEmail = order.getUser().getEmail(); // Already encrypted in DB

        // 3. Build and Publish CreateGhnOrderMessage
        CreateGhnOrderMessage msg = CreateGhnOrderMessage.builder()
                .orderCode(order.getOrderCode())
                .receiverName(order.getReceiverName())
                .recipientPhone(encryptedPhone)
                .toAddress(fullAddress)
                .toWardCode(order.getWard().getWardCode())
                .toDistrictId(order.getDistrict().getDistrictId())
                .toWardName(order.getWard().getWardName())
                .toDistrictName(order.getDistrict().getDistrictName())
                .toProvinceName(order.getProvince().getProvinceName())
                .paymentMethod(order.getPaymentMethod().name())
                .codAmount(codAmount)
                .totalWeight(totalWeight)
                .totalLength(maxLength)
                .totalWidth(maxWidth)
                .totalHeight(totalHeight)
                .insuranceValue(insuranceVal)
                .items(itemInfos)
                .userEmail(encryptedEmail)
                .userName(order.getUser().getProfile() != null ? order.getUser().getProfile().getFullName() : order.getUser().getUsername())
                .build();

        outboxPublisher.publish(
                QueueConstants.CREATE_GHN_ORDER,
                msg,
                "urn:message:InkPulse.Worker.Features.Order.Messages:CreateGhnOrderMessage");

        log.info("Successfully published CreateGhnOrderMessage to Outbox for order: {}", order.getOrderCode());

        // Save Order Event (Order Event Store)
        OrderEvent orderEvent = OrderEvent.builder()
                .order(order)
                .eventType(OrderEventType.ORDER_PACKED)
                .eventData(JsonHelper.serializeSafe(command))
                .createdBy(command.getAdminUserId())
                .build();
        orderEventRepository.save(orderEvent);

        return ConfirmPackOrderResponse.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .message(OrderMessageConstants.CONFIRM_PACK_SUCCESS)
                .build();
    }
}
