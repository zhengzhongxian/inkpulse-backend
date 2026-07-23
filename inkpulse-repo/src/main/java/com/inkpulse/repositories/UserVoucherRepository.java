package com.inkpulse.repositories;

import com.inkpulse.entities.UserVoucher;
import com.inkpulse.entities.enums.UserVoucherStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserVoucherRepository extends JpaRepository<UserVoucher, UUID>, JpaSpecificationExecutor<UserVoucher> {
    long countByUserIdAndVoucherId(UUID userId, UUID voucherId);

    @Query("SELECT uv.voucher.id FROM UserVoucher uv WHERE uv.user.id = :userId GROUP BY uv.voucher.id, uv.voucher.maxUsesPerUser HAVING COUNT(uv.id) >= uv.voucher.maxUsesPerUser")
    List<UUID> findFullyAcquiredVoucherIds(@Param("userId") UUID userId);

    Optional<UserVoucher> findFirstByUserIdAndVoucherIdAndStatus(UUID userId, UUID voucherId, UserVoucherStatus status);

    List<UserVoucher> findAllByUserId(UUID userId);
}
