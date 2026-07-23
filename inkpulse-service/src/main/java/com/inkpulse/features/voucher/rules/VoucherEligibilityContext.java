package com.inkpulse.features.voucher.rules;

import com.inkpulse.entities.enums.VoucherDiscountType;
import com.inkpulse.entities.enums.VoucherTargetType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
public class VoucherEligibilityContext {
    private final UUID existingVoucherId; // null for Create, non-null for Update
    private final String voucherCode;
    private final VoucherDiscountType discountType;
    private final BigDecimal discountValue;
    private final BigDecimal minOrderValue;
    private final Integer maxUses;
    private final Integer maxUsesPerUser;
    private final Integer coinCost;
    private final VoucherTargetType targetType;
    private final List<UUID> targetIds;
    private final BigDecimal maxDiscountAmount;
    private final ZonedDateTime startDate;
    private final ZonedDateTime endDate;
}
