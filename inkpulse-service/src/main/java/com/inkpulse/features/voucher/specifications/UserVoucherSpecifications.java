package com.inkpulse.features.voucher.specifications;

import com.inkpulse.entities.UserVoucher;
import com.inkpulse.entities.enums.UserVoucherStatus;
import org.springframework.data.jpa.domain.Specification;
import java.time.ZonedDateTime;
import java.util.UUID;

public final class UserVoucherSpecifications {

    private UserVoucherSpecifications() {
    }

    public static Specification<UserVoucher> hasUserId(UUID userId) {
        return (root, query, cb) -> userId == null ? null : cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<UserVoucher> hasStatus(UserVoucherStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<UserVoucher> isActiveOnly() {
        return (root, query, cb) -> {
            ZonedDateTime now = ZonedDateTime.now();
            return cb.and(
                cb.equal(root.get("status"), UserVoucherStatus.UNUSED),
                cb.equal(root.get("voucher").get("isActive"), true),
                cb.greaterThanOrEqualTo(root.get("voucher").get("endDate"), now)
            );
        };
    }
}
