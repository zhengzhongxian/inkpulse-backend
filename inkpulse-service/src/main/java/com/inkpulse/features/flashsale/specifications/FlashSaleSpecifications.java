package com.inkpulse.features.flashsale.specifications;

import com.inkpulse.entities.FlashSale;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

public final class FlashSaleSpecifications {

    private FlashSaleSpecifications() {
    }

    public static Specification<FlashSale> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return null;
            }
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            // Check campaign name OR join items to search book title / isbn
            query.distinct(true);
            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.join("items").join("bookEdition").join("book").get("title")), pattern),
                    cb.like(cb.lower(root.join("items").join("bookEdition").get("isbn")), pattern)
            );
        };
    }

    public static Specification<FlashSale> isActive(Boolean active) {
        return (root, query, cb) -> active == null ? null : cb.equal(root.get("isActive"), active);
    }

    public static Specification<FlashSale> startDateBetween(ZonedDateTime from, ZonedDateTime to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return null;
            if (from != null && to != null) return cb.between(root.get("startDate"), from, to);
            if (from != null) return cb.greaterThanOrEqualTo(root.get("startDate"), from);
            return cb.lessThanOrEqualTo(root.get("startDate"), to);
        };
    }

    public static Specification<FlashSale> endDateBetween(ZonedDateTime from, ZonedDateTime to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return null;
            if (from != null && to != null) return cb.between(root.get("endDate"), from, to);
            if (from != null) return cb.greaterThanOrEqualTo(root.get("endDate"), from);
            return cb.lessThanOrEqualTo(root.get("endDate"), to);
        };
    }

    public static Specification<FlashSale> createdBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return null;
            if (from != null && to != null) return cb.between(root.get("createdAt"), from, to);
            if (from != null) return cb.greaterThanOrEqualTo(root.get("createdAt"), from);
            return cb.lessThanOrEqualTo(root.get("createdAt"), to);
        };
    }

    public static Specification<FlashSale> stockBetween(Integer min, Integer max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            query.distinct(true);
            if (min != null && max != null) return cb.between(root.join("items").get("flashSaleStock"), min, max);
            if (min != null) return cb.greaterThanOrEqualTo(root.join("items").get("flashSaleStock"), min);
            return cb.lessThanOrEqualTo(root.join("items").get("flashSaleStock"), max);
        };
    }

    public static Specification<FlashSale> discountAmountBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            query.distinct(true);
            if (min != null && max != null) return cb.between(root.join("items").get("discountAmount"), min, max);
            if (min != null) return cb.greaterThanOrEqualTo(root.join("items").get("discountAmount"), min);
            return cb.lessThanOrEqualTo(root.join("items").get("discountAmount"), max);
        };
    }

    public static Specification<FlashSale> isCurrentlyRunning() {
        return (root, query, cb) -> {
            ZonedDateTime now = ZonedDateTime.now();
            return cb.and(
                    cb.equal(root.get("isActive"), true),
                    cb.lessThanOrEqualTo(root.get("startDate"), now),
                    cb.greaterThanOrEqualTo(root.get("endDate"), now)
            );
        };
    }

    public static Specification<FlashSale> isActiveAndNotEnded() {
        return (root, query, cb) -> {
            ZonedDateTime now = ZonedDateTime.now();
            return cb.and(
                    cb.equal(root.get("isActive"), true),
                    cb.greaterThanOrEqualTo(root.get("endDate"), now)
            );
        };
    }
}
