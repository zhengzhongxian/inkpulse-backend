package com.inkpulse.features.voucher.rules;

import com.inkpulse.constants.message.VoucherMessageConstants;
import com.inkpulse.entities.Voucher;
import com.inkpulse.pipeline.EligibilityContext;
import com.inkpulse.pipeline.IEligibilityRule;
import com.inkpulse.repositories.VoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class VoucherCodeUniqueRule implements IEligibilityRule<VoucherEligibilityContext> {

    private final VoucherRepository voucherRepository;

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public void evaluate(EligibilityContext<VoucherEligibilityContext> context) {
        VoucherEligibilityContext entity = context.getEntity();
        if (entity.getVoucherCode() == null || entity.getVoucherCode().trim().isEmpty()) {
            context.reject("Mã giảm giá không được để trống!");
            return;
        }
        Optional<Voucher> existing = voucherRepository.findByVoucherCode(entity.getVoucherCode().trim());
        if (existing.isPresent()) {
            Voucher existingVoucher = existing.get();
            if (entity.getExistingVoucherId() == null || !existingVoucher.getId().equals(entity.getExistingVoucherId())) {
                context.reject(VoucherMessageConstants.CODE_ALREADY_EXISTS);
            }
        }
    }
}
