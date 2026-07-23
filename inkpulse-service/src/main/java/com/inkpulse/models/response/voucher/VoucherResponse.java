package com.inkpulse.models.response.voucher;

public record VoucherResponse(
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
    boolean isActive,
    int coinCost,
    String targetType,
    String maxDiscountAmount,
    String createdAt
) {}
