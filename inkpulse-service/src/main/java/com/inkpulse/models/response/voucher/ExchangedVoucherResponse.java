package com.inkpulse.models.response.voucher;

public record ExchangedVoucherResponse(
    String userVoucherId,
    String status,
    String acquiredAt,
    String usedAt,
    PublicVoucherResponse voucher
) {}
