package com.inkpulse.features.voucher.strategies;

import com.inkpulse.entities.BookEdition;
import com.inkpulse.entities.Voucher;
import com.inkpulse.entities.VoucherEdition;
import com.inkpulse.entities.enums.VoucherTargetType;
import com.inkpulse.repositories.VoucherEditionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EditionVoucherTargetStrategy implements VoucherTargetStrategy {

    private final VoucherEditionRepository voucherEditionRepository;

    @Override
    public VoucherTargetType getTargetType() {
        return VoucherTargetType.EDITION;
    }

    @Override
    public boolean isEligible(Voucher voucher, BookEdition edition) {
        UUID editionId = edition.getId();
        List<VoucherEdition> voucherEditions = voucherEditionRepository.findByVoucherId(voucher.getId());
        return voucherEditions.stream()
                .anyMatch(ve -> ve.getBookEdition() != null && ve.getBookEdition().getId().equals(editionId));
    }
}
