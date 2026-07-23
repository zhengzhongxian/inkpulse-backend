package com.inkpulse.features.voucher.strategies;

import com.inkpulse.entities.BookEdition;
import com.inkpulse.entities.Voucher;
import com.inkpulse.entities.enums.VoucherTargetType;

public interface VoucherTargetStrategy {
    VoucherTargetType getTargetType();
    boolean isEligible(Voucher voucher, BookEdition edition);
}
