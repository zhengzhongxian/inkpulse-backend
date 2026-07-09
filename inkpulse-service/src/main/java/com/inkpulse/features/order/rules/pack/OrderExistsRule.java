package com.inkpulse.features.order.rules.pack;

import com.inkpulse.constants.message.OrderMessageConstants;
import com.inkpulse.entities.Order;
import com.inkpulse.features.order.rules.ConfirmPackOrderContext;
import com.inkpulse.pipeline.EligibilityContext;
import com.inkpulse.pipeline.IEligibilityRule;
import com.inkpulse.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrderExistsRule implements IEligibilityRule<ConfirmPackOrderContext> {

    private final OrderRepository orderRepository;

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public void evaluate(EligibilityContext<ConfirmPackOrderContext> context) {
        ConfirmPackOrderContext ctx = context.getEntity();
        Optional<Order> orderOpt = orderRepository.findByOrderCode(ctx.getCommand().getOrderCode());
        if (orderOpt.isEmpty()) {
            context.reject(OrderMessageConstants.ORDER_NOT_FOUND);
            return;
        }
        ctx.setOrder(orderOpt.get());
    }
}
