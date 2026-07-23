package com.inkpulse.features.voucher.rules;

import com.inkpulse.constants.message.VoucherMessageConstants;
import com.inkpulse.entities.enums.VoucherTargetType;
import com.inkpulse.pipeline.EligibilityContext;
import com.inkpulse.pipeline.IEligibilityRule;
import com.inkpulse.repositories.CategoryRepository;
import com.inkpulse.repositories.BookRepository;
import com.inkpulse.repositories.BookEditionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VoucherTargetValidationRule implements IEligibilityRule<VoucherEligibilityContext> {

    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;
    private final BookEditionRepository bookEditionRepository;

    @Override
    public int getOrder() {
        return 4;
    }

    @Override
    public void evaluate(EligibilityContext<VoucherEligibilityContext> context) {
        VoucherEligibilityContext entity = context.getEntity();
        VoucherTargetType targetType = entity.getTargetType();
        List<UUID> targetIds = entity.getTargetIds();

        if (targetType == null) {
            context.reject("Đối tượng áp dụng không được để trống!");
            return;
        }

        if (targetType == VoucherTargetType.ALL || targetType == VoucherTargetType.SHIPPING) {
            return;
        }

        if (targetIds == null || targetIds.isEmpty()) {
            context.reject(VoucherMessageConstants.TARGET_IDS_REQUIRED);
            return;
        }

        int expectedCount = targetIds.size();
        int actualCount = 0;

        switch (targetType) {
            case CATEGORY -> actualCount = categoryRepository.findAllById(targetIds).size();
            case BOOK -> actualCount = bookRepository.findAllById(targetIds).size();
            case EDITION -> actualCount = bookEditionRepository.findAllById(targetIds).size();
        }

        if (actualCount != expectedCount) {
            context.reject(VoucherMessageConstants.TARGET_IDS_NOT_FOUND);
        }
    }
}
