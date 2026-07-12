package com.inkpulse.features.order.rules.returnorder;

import com.inkpulse.constants.message.OrderMessageConstants;
import com.inkpulse.entities.Order;
import com.inkpulse.entities.enums.OrderStatus;
import com.inkpulse.features.order.rules.ReturnOrderContext;
import com.inkpulse.pipeline.EligibilityContext;
import com.inkpulse.pipeline.IEligibilityRule;
import com.inkpulse.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ReturnOrderValidationRule implements IEligibilityRule<ReturnOrderContext> {

    private final OrderRepository orderRepository;

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public void evaluate(EligibilityContext<ReturnOrderContext> context) {
        ReturnOrderContext ctx = context.getEntity();
        Optional<Order> orderOpt = orderRepository.findByOrderCode(ctx.getCommand().getOrderCode());
        
        if (orderOpt.isEmpty()) {
            context.reject(OrderMessageConstants.ORDER_NOT_FOUND);
            return;
        }

        Order order = orderOpt.get();
        ctx.setOrder(order);

        if (order.getOrderStatus() != OrderStatus.SHIPPED) {
            context.reject("Chỉ có thể yêu cầu chuyển hoàn cho đơn hàng đang giao (SHIPPED). Trạng thái hiện tại: " + order.getOrderStatus());
        }
    }
}
