package com.inkpulse.models.response.voucher;

import java.util.List;

public record VoucherDetailResponse(
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
    String createdAt,
    List<VoucherTargetItemResponse> targetItems
) {
    public record VoucherTargetItemResponse(
        String id,
        String name
    ) {}
}
