package com.inkpulse.features.voucher.rules;

import com.inkpulse.constants.message.VoucherMessageConstants;
import com.inkpulse.pipeline.EligibilityContext;
import com.inkpulse.pipeline.IEligibilityRule;
import org.springframework.stereotype.Component;
import java.time.ZonedDateTime;

@Component
public class VoucherDateValidationRule implements IEligibilityRule<VoucherEligibilityContext> {

    @Override
    public int getOrder() {
        return 2;
    }

    @Override
    public void evaluate(EligibilityContext<VoucherEligibilityContext> context) {
        VoucherEligibilityContext entity = context.getEntity();
        ZonedDateTime start = entity.getStartDate();
        ZonedDateTime end = entity.getEndDate();

        if (start == null || end == null) {
            context.reject("Thời gian bắt đầu và kết thúc không được để trống!");
            return;
        }

        if (start.isAfter(end) || start.isEqual(end)) {
            context.reject(VoucherMessageConstants.START_DATE_AFTER_END_DATE);
            return;
        }

        if (entity.getExistingVoucherId() == null) {
            ZonedDateTime now = ZonedDateTime.now();
            if (start.isBefore(now.minusMinutes(5))) {
                context.reject(VoucherMessageConstants.START_DATE_PAST);
                return;
            }
            if (end.isBefore(now)) {
                context.reject(VoucherMessageConstants.END_DATE_PAST);
            }
        } else {
            ZonedDateTime now = ZonedDateTime.now();
            if (end.isBefore(now)) {
                context.reject(VoucherMessageConstants.END_DATE_PAST);
            }
        }
    }
}
