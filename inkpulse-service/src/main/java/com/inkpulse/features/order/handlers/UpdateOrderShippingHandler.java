package com.inkpulse.features.order.handlers;

import com.inkpulse.constants.message.OrderMessageConstants;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.Order;
import com.inkpulse.entities.OrderEvent;
import com.inkpulse.entities.enums.OrderStatus;
import com.inkpulse.entities.enums.OrderEventType;
import com.inkpulse.features.order.commands.UpdateOrderShippingCommand;
import com.inkpulse.repositories.OrderRepository;
import com.inkpulse.repositories.OrderEventRepository;
import com.inkpulse.service.ghn.IGhnShippingService;
import com.inkpulse.corehelpers.JsonHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateOrderShippingHandler implements Command.CommandHandler<UpdateOrderShippingCommand, Void> {

    private final OrderRepository orderRepository;
    private final IGhnShippingService ghnShippingService;
    private final OrderEventRepository orderEventRepository;

    @Override
    @Transactional
    public Void handle(UpdateOrderShippingCommand command) {
        log.info("Handling UpdateOrderShippingCommand for order: {}", command.getOrderCode());

        // 1. Fetch order
        Optional<Order> orderOpt = orderRepository.findByOrderCode(command.getOrderCode());
        if (orderOpt.isEmpty()) {
            throw new BusinessValidationException(OrderMessageConstants.ORDER_NOT_FOUND, "ORDER_NOT_FOUND");
        }

        Order order = orderOpt.get();

        // 2. Validate Order Status (must be PROCESSING)
        if (order.getOrderStatus() != OrderStatus.PROCESSING) {
            throw new BusinessValidationException(OrderMessageConstants.ORDER_NOT_PROCESSING, "INVALID_ORDER_STATE");
        }

        if (order.getGhnOrderCode() == null || order.getGhnOrderCode().trim().isEmpty()) {
            throw new BusinessValidationException(OrderMessageConstants.SHIPPING_UPDATE_NOT_PACKED, OrderMessageConstants.CODE_GHN_CODE_MISSING);
        }

        // 3. Call GHN Update API directly
        ghnShippingService.updateShippingOrder(
                order.getGhnOrderCode(),
                command.getNote(),
                command.getRequiredNote(),
                command.getWeight(),
                command.getLength(),
                command.getWidth(),
                command.getHeight()
        );

        // Save Order Event (Order Event Store)
        OrderEvent orderEvent = OrderEvent.builder()
                .order(order)
                .eventType(OrderEventType.SHIPPING_UPDATED)
                .eventData(JsonHelper.serializeSafe(command))
                .createdBy(UUID.fromString(command.getAdminUserId()))
                .build();
        orderEventRepository.save(orderEvent);

        log.info("Successfully updated order shipping information on GHN for order: {}", order.getOrderCode());
        return null;
    }
}
