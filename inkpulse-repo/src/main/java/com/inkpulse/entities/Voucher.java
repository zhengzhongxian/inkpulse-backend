package com.inkpulse.entities;

import com.inkpulse.entities.enums.VoucherDiscountType;
import com.inkpulse.entities.enums.VoucherTargetType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "vouchers")
@SQLRestriction("is_deleted = false")
@AttributeOverride(name = "id", column = @Column(name = "voucher_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Voucher extends BaseAuditableEntity<UUID> {

    @Column(name = "start_date", nullable = false)
    private ZonedDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private ZonedDateTime endDate;

    @Column(name = "voucher_code", nullable = false, unique = true, length = 100)
    private String voucherCode;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 50)
    private VoucherDiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal discountValue;

    @Column(name = "min_order_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal minOrderValue;

    @Column(name = "max_uses", nullable = false)
    private Integer maxUses;

    @Column(name = "used_count", nullable = false)
    @Builder.Default
    private Integer usedCount = 0;

    @Column(name = "max_uses_per_user", nullable = false)
    @Builder.Default
    private Integer maxUsesPerUser = 1;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "coin_cost", nullable = false)
    @Builder.Default
    private Integer coinCost = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 50)
    private VoucherTargetType targetType;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;
}
