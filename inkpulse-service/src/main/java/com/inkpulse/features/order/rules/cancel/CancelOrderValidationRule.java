package com.inkpulse.features.order.rules.cancel;

import com.inkpulse.constants.message.OrderMessageConstants;
import com.inkpulse.entities.Order;
import com.inkpulse.entities.enums.OrderStatus;
import com.inkpulse.features.order.rules.CancelOrderContext;
import com.inkpulse.pipeline.EligibilityContext;
import com.inkpulse.pipeline.IEligibilityRule;
import com.inkpulse.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CancelOrderValidationRule implements IEligibilityRule<CancelOrderContext> {

    private final OrderRepository orderRepository;

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public void evaluate(EligibilityContext<CancelOrderContext> context) {
        CancelOrderContext ctx = context.getEntity();
        Optional<Order> orderOpt = orderRepository.findByOrderCode(ctx.getCommand().getOrderCode());
        
        if (orderOpt.isEmpty()) {
            context.reject(OrderMessageConstants.ORDER_NOT_FOUND);
            return;
        }

        Order order = orderOpt.get();
        ctx.setOrder(order);

        OrderStatus status = order.getOrderStatus();
        boolean isAdmin = ctx.getCommand().isAdmin();

        if (isAdmin) {
            // Admin can cancel PENDING_PAYMENT, PENDING, or PROCESSING orders
            if (status != OrderStatus.PENDING_PAYMENT && status != OrderStatus.PENDING && status != OrderStatus.PROCESSING) {
                context.reject("Không thể hủy đơn hàng ở trạng thái hiện tại (" + status + ")");
            }
        } else {
            // Customer can only cancel PENDING_PAYMENT or PENDING orders (not packed/processing yet)
            if (status != OrderStatus.PENDING_PAYMENT && status != OrderStatus.PENDING) {
                context.reject("Đơn hàng đã được đóng gói hoặc đang giao, khách hàng không thể tự hủy.");
            }
        }
    }
}
