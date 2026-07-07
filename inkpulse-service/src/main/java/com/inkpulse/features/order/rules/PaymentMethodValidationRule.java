package com.inkpulse.features.order.rules;

import com.inkpulse.constants.message.OrderMessageConstants;
import com.inkpulse.entities.enums.PaymentMethod;
import com.inkpulse.pipeline.EligibilityContext;
import com.inkpulse.pipeline.IEligibilityRule;
import org.springframework.stereotype.Component;

@Component
public class PaymentMethodValidationRule implements IEligibilityRule<CreateOrderContext> {

    @Override
    public int getOrder() {
        return 4;
    }

    @Override
    public void evaluate(EligibilityContext<CreateOrderContext> context) {
        CreateOrderContext ctx = context.getEntity();
        String method = ctx.getCommand().getPaymentMethod();

        if (method == null || method.trim().isEmpty()) {
            context.reject(OrderMessageConstants.INVALID_PAYMENT_METHOD);
            return;
        }

        try {
            PaymentMethod.valueOf(method.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            context.reject(OrderMessageConstants.INVALID_PAYMENT_METHOD);
        }
    }
}
