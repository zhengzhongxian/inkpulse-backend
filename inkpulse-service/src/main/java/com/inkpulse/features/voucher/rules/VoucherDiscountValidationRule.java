package com.inkpulse.features.voucher.rules;

import com.inkpulse.constants.message.VoucherMessageConstants;
import com.inkpulse.entities.enums.VoucherDiscountType;
import com.inkpulse.pipeline.EligibilityContext;
import com.inkpulse.pipeline.IEligibilityRule;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class VoucherDiscountValidationRule implements IEligibilityRule<VoucherEligibilityContext> {

    @Override
    public int getOrder() {
        return 3;
    }

    @Override
    public void evaluate(EligibilityContext<VoucherEligibilityContext> context) {
        VoucherEligibilityContext entity = context.getEntity();

        if (entity.getDiscountType() == null) {
            context.reject("Loại giảm giá không được để trống!");
            return;
        }

        BigDecimal discountValue = entity.getDiscountValue();
        if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            context.reject(VoucherMessageConstants.DISCOUNT_VALUE_INVALID);
            return;
        }

        if (entity.getDiscountType() == VoucherDiscountType.PERCENTAGE) {
            if (discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
                context.reject(VoucherMessageConstants.DISCOUNT_PERCENTAGE_EXCEEDED);
                return;
            }
            if (entity.getMaxDiscountAmount() != null && entity.getMaxDiscountAmount().compareTo(BigDecimal.ZERO) < 0) {
                context.reject("Số tiền giảm giá tối đa không được nhỏ hơn 0");
                return;
            }
        }

        if (entity.getMinOrderValue() == null || entity.getMinOrderValue().compareTo(BigDecimal.ZERO) < 0) {
            context.reject(VoucherMessageConstants.MIN_ORDER_VALUE_INVALID);
            return;
        }

        if (entity.getMaxUses() == null || entity.getMaxUses() <= 0) {
            context.reject(VoucherMessageConstants.MAX_USES_INVALID);
            return;
        }

        if (entity.getMaxUsesPerUser() == null || entity.getMaxUsesPerUser() <= 0) {
            context.reject(VoucherMessageConstants.MAX_USES_PER_USER_INVALID);
            return;
        }

        if (entity.getCoinCost() == null || entity.getCoinCost() < 0) {
            context.reject(VoucherMessageConstants.COIN_COST_INVALID);
        }
    }
}
