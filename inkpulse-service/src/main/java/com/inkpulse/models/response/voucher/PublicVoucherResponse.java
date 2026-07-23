package com.inkpulse.models.response.voucher;

public record PublicVoucherResponse(
    String voucherId,
    String startDate,
    String endDate,
    String voucherCode,
    String description,
    String discountType,
    String discountValue,
    String minOrderValue,
    int maxUses,
    int usedCount,
    int maxUsesPerUser,
    int coinCost,
    String targetType,
    String maxDiscountAmount
) {}
