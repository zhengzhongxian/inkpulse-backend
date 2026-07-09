package com.inkpulse.features.order.rules.pack;

import com.inkpulse.constants.message.OrderMessageConstants;
import com.inkpulse.entities.Order;
import com.inkpulse.entities.enums.PaymentMethod;
import com.inkpulse.entities.enums.PaymentStatus;
import com.inkpulse.features.order.rules.ConfirmPackOrderContext;
import com.inkpulse.pipeline.EligibilityContext;
import com.inkpulse.pipeline.IEligibilityRule;
import org.springframework.stereotype.Component;

@Component
public class OrderIsPaidRule implements IEligibilityRule<ConfirmPackOrderContext> {

    @Override
    public int getOrder() {
        return 2;
    }

    @Override
    public void evaluate(EligibilityContext<ConfirmPackOrderContext> context) {
        ConfirmPackOrderContext ctx = context.getEntity();
        Order order = ctx.getOrder();
        if (order == null) {
            context.reject(OrderMessageConstants.ORDER_NOT_FOUND);
            return;
        }

        if (order.getPaymentMethod() != PaymentMethod.COD && order.getPaymentStatus() != PaymentStatus.PAID) {
            context.reject(OrderMessageConstants.ORDER_NOT_PAID);
        }
    }
}
