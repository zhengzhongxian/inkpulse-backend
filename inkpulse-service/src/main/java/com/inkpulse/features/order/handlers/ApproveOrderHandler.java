package com.inkpulse.features.order.handlers;

import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.Order;
import com.inkpulse.entities.OrderLog;
import com.inkpulse.entities.OrderEvent;
import com.inkpulse.entities.enums.OrderStatus;
import com.inkpulse.entities.enums.OrderEventType;
import com.inkpulse.features.order.commands.ApproveOrderCommand;
import com.inkpulse.repositories.OrderLogRepository;
import com.inkpulse.repositories.OrderRepository;
import com.inkpulse.repositories.OrderEventRepository;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.corehelpers.exceptions.ResourceNotFoundException;
import com.inkpulse.corehelpers.JsonHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApproveOrderHandler implements Command.CommandHandler<ApproveOrderCommand, Void> {

    private final OrderRepository orderRepository;
    private final OrderLogRepository orderLogRepository;
    private final OrderEventRepository orderEventRepository;

    @Override
    @Transactional
    public Void handle(ApproveOrderCommand command) {
        log.info("Handling ApproveOrderCommand for orderCode: {} by admin: {}", command.orderCode(), command.adminUserId());

        Order order = orderRepository.findByOrderCode(command.orderCode())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderCode", command.orderCode()));

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new BusinessValidationException("Đơn hàng không ở trạng thái chờ duyệt (PENDING)!", "INVALID_ORDER_STATUS");
        }

        OrderStatus fromStatus = order.getOrderStatus();
        OrderStatus toStatus = OrderStatus.PROCESSING;

        // Update status
        order.setOrderStatus(toStatus);
        orderRepository.save(order);

        // Save Order Event
        OrderEvent orderEvent = OrderEvent.builder()
                .order(order)
                .eventType(OrderEventType.ORDER_APPROVED)
                .eventData(JsonHelper.serializeSafe(command))
                .createdBy(command.adminUserId())
                .build();
        orderEventRepository.save(orderEvent);

        // Insert Order Log
        OrderLog orderLog = OrderLog.builder()
                .order(order)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .changedBy(command.adminUserId())
                .adminNote("Admin duyệt đơn hàng (COD)")
                .userNote("Đơn hàng của bạn đã được duyệt và chuyển sang đóng gói")
                .build();
        orderLogRepository.save(orderLog);

        log.info("Successfully approved order: {}", command.orderCode());
        return null;
    }
}
