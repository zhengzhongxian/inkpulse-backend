package com.inkpulse.features.voucher.dto;

import com.inkpulse.cache.CacheSection;
import com.inkpulse.cache.Cacheable;
import com.inkpulse.constants.KeyConstants;
import java.util.List;

@CacheSection(KeyConstants.SECTION_VOUCHER_DETAIL)
public record VoucherDetailCacheDto(
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
    List<VoucherTargetItemCacheDto> targetItems
) implements Cacheable {

    @Override
    public String cacheId() {
        return voucherId;
    }

    public record VoucherTargetItemCacheDto(
        String id,
        String name
    ) {}
}
