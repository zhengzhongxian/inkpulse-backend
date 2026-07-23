package com.inkpulse.features.voucher.strategies;

import com.inkpulse.entities.BookEdition;
import com.inkpulse.entities.Category;
import com.inkpulse.entities.Voucher;
import com.inkpulse.entities.VoucherCategory;
import com.inkpulse.entities.enums.VoucherTargetType;
import com.inkpulse.repositories.VoucherCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CategoryVoucherTargetStrategy implements VoucherTargetStrategy {

    private final VoucherCategoryRepository voucherCategoryRepository;

    @Override
    public VoucherTargetType getTargetType() {
        return VoucherTargetType.CATEGORY;
    }

    @Override
    public boolean isEligible(Voucher voucher, BookEdition edition) {
        if (edition.getBook() == null || edition.getBook().getCategories() == null) {
            return false;
        }
        
        Set<UUID> bookCategoryIds = edition.getBook().getCategories().stream()
                .map(Category::getId)
                .collect(Collectors.toSet());

        List<VoucherCategory> voucherCategories = voucherCategoryRepository.findByVoucherId(voucher.getId());
        return voucherCategories.stream()
                .anyMatch(vc -> vc.getCategory() != null && bookCategoryIds.contains(vc.getCategory().getId()));
    }
}
