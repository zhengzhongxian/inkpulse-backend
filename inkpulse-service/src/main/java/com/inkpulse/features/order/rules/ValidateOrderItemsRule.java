package com.inkpulse.features.order.rules;

import com.inkpulse.constants.message.OrderMessageConstants;
import com.inkpulse.models.request.order.OrderItemRequest;
import com.inkpulse.pipeline.EligibilityContext;
import com.inkpulse.pipeline.IEligibilityRule;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ValidateOrderItemsRule implements IEligibilityRule<CreateOrderContext> {

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public void evaluate(EligibilityContext<CreateOrderContext> context) {
        CreateOrderContext ctx = context.getEntity();
        List<OrderItemRequest> items = ctx.getCommand().getItems();

        if (items == null || items.isEmpty()) {
            context.reject(OrderMessageConstants.INVALID_ITEMS);
            return;
        }

        if (items.size() > 20) {
            context.reject(OrderMessageConstants.MAX_ITEMS_EXCEEDED);
            return;
        }

        for (OrderItemRequest item : items) {
            if (item.getQuantity() <= 0) {
                context.reject(OrderMessageConstants.INVALID_QUANTITY);
                return;
            }
            if (item.getEditionId() == null) {
                context.reject(OrderMessageConstants.EDITION_NOT_FOUND);
                return;
            }
        }
    }
}
