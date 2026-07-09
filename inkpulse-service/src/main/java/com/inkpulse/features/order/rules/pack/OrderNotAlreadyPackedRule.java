package com.inkpulse.features.order.rules.pack;

import com.inkpulse.constants.message.OrderMessageConstants;
import com.inkpulse.entities.Order;
import com.inkpulse.features.order.rules.ConfirmPackOrderContext;
import com.inkpulse.pipeline.EligibilityContext;
import com.inkpulse.pipeline.IEligibilityRule;
import org.springframework.stereotype.Component;

@Component
public class OrderNotAlreadyPackedRule implements IEligibilityRule<ConfirmPackOrderContext> {

    @Override
    public int getOrder() {
        return 3;
    }

    @Override
    public void evaluate(EligibilityContext<ConfirmPackOrderContext> context) {
        ConfirmPackOrderContext ctx = context.getEntity();
        Order order = ctx.getOrder();
        if (order == null) {
            context.reject(OrderMessageConstants.ORDER_NOT_FOUND);
            return;
        }

        if (order.getGhnOrderCode() != null && !order.getGhnOrderCode().trim().isEmpty()) {
            context.reject(OrderMessageConstants.ORDER_ALREADY_PACKED);
        }
    }
}
