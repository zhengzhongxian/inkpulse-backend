package com.inkpulse.features.voucher.strategies;

import com.inkpulse.entities.BookEdition;
import com.inkpulse.entities.Voucher;
import com.inkpulse.entities.enums.VoucherTargetType;
import org.springframework.stereotype.Component;

@Component
public class ShippingVoucherTargetStrategy implements VoucherTargetStrategy {

    @Override
    public VoucherTargetType getTargetType() {
        return VoucherTargetType.SHIPPING;
    }

    @Override
    public boolean isEligible(Voucher voucher, BookEdition edition) {
        return false;
    }
}
