package com.inkpulse.features.order.rules.pack;

import com.inkpulse.constants.message.OrderMessageConstants;
import com.inkpulse.entities.Order;
import com.inkpulse.entities.enums.OrderStatus;
import com.inkpulse.features.order.rules.ConfirmPackOrderContext;
import com.inkpulse.pipeline.EligibilityContext;
import com.inkpulse.pipeline.IEligibilityRule;
import org.springframework.stereotype.Component;

@Component
public class OrderIsProcessingRule implements IEligibilityRule<ConfirmPackOrderContext> {

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public void evaluate(EligibilityContext<ConfirmPackOrderContext> context) {
        ConfirmPackOrderContext ctx = context.getEntity();
        Order order = ctx.getOrder();
        if (order == null) {
            context.reject(OrderMessageConstants.ORDER_NOT_FOUND);
            return;
        }

        if (order.getOrderStatus() != OrderStatus.PROCESSING) {
            context.reject(OrderMessageConstants.ORDER_NOT_PROCESSING);
        }
    }
}
