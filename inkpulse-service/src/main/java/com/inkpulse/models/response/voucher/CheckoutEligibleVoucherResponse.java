package com.inkpulse.models.response.voucher;

import java.math.BigDecimal;

public record CheckoutEligibleVoucherResponse(
    String userVoucherId,
    String status,
    PublicVoucherResponse voucher,
    boolean eligible,
    String reason,
    BigDecimal calculatedDiscount
) {}
