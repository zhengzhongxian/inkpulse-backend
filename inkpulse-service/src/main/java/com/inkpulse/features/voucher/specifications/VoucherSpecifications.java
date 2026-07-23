package com.inkpulse.features.voucher.specifications;

import com.inkpulse.entities.Voucher;
import com.inkpulse.entities.enums.VoucherDiscountType;
import com.inkpulse.entities.enums.VoucherTargetType;
import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

public final class VoucherSpecifications {

    private VoucherSpecifications() {
    }

    public static Specification<Voucher> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return null;
            }
            return cb.like(cb.lower(root.get("voucherCode")), "%" + keyword.trim().toLowerCase() + "%");
        };
    }

    public static Specification<Voucher> hasDiscountType(VoucherDiscountType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("discountType"), type);
    }

    public static Specification<Voucher> hasTargetType(VoucherTargetType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("targetType"), type);
    }

    public static Specification<Voucher> discountValueBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min != null && max != null) return cb.between(root.get("discountValue"), min, max);
            if (min != null) return cb.greaterThanOrEqualTo(root.get("discountValue"), min);
            return cb.lessThanOrEqualTo(root.get("discountValue"), max);
        };
    }

    public static Specification<Voucher> maxUsesBetween(Integer min, Integer max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min != null && max != null) return cb.between(root.get("maxUses"), min, max);
            if (min != null) return cb.greaterThanOrEqualTo(root.get("maxUses"), min);
            return cb.lessThanOrEqualTo(root.get("maxUses"), max);
        };
    }

    public static Specification<Voucher> minOrderValueBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min != null && max != null) return cb.between(root.get("minOrderValue"), min, max);
            if (min != null) return cb.greaterThanOrEqualTo(root.get("minOrderValue"), min);
            return cb.lessThanOrEqualTo(root.get("minOrderValue"), max);
        };
    }

    public static Specification<Voucher> coinCostBetween(Integer min, Integer max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min != null && max != null) return cb.between(root.get("coinCost"), min, max);
            if (min != null) return cb.greaterThanOrEqualTo(root.get("coinCost"), min);
            return cb.lessThanOrEqualTo(root.get("coinCost"), max);
        };
    }

    public static Specification<Voucher> createdBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return null;
            if (from != null && to != null) return cb.between(root.get("createdAt"), from, to);
            if (from != null) return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            return cb.lessThanOrEqualTo(root.get("createdAt"), to);
        };
    }

    public static Specification<Voucher> startDateBetween(ZonedDateTime from, ZonedDateTime to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return null;
            if (from != null && to != null) return cb.between(root.get("startDate"), from, to);
            if (from != null) return cb.greaterThanOrEqualTo(root.get("startDate"), from);
            return cb.lessThanOrEqualTo(root.get("startDate"), to);
        };
    }

    public static Specification<Voucher> endDateBetween(ZonedDateTime from, ZonedDateTime to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return null;
            if (from != null && to != null) return cb.between(root.get("endDate"), from, to);
            if (from != null) return cb.greaterThanOrEqualTo(root.get("endDate"), from);
            return cb.lessThanOrEqualTo(root.get("endDate"), to);
        };
    }

    public static Specification<Voucher> isActive(Boolean active) {
        return (root, query, cb) -> active == null ? null : cb.equal(root.get("isActive"), active);
    }

    public static Specification<Voucher> isCurrentlyValid() {
        return (root, query, cb) -> {
            ZonedDateTime now = ZonedDateTime.now();
            return cb.and(
                cb.equal(root.get("isActive"), true),
                cb.greaterThanOrEqualTo(root.get("endDate"), now),
                cb.lessThan(root.get("usedCount"), root.get("maxUses"))
            );
        };
    }

    public static Specification<Voucher> coinCostLessThanOrEqual(Integer maxCost) {
        return (root, query, cb) -> maxCost == null ? null : cb.lessThanOrEqualTo(root.get("coinCost"), maxCost);
    }

    public static Specification<Voucher> idNotIn(java.util.Collection<java.util.UUID> ids) {
        return (root, query, cb) -> {
            if (ids == null || ids.isEmpty()) {
                return null;
            }
            return cb.not(root.get("id").in(ids));
        };
    }
}
