package com.inkpulse.repositories;

import com.inkpulse.entities.RefundRequest;
import com.inkpulse.entities.enums.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, UUID> {

    @Modifying
    @Query("UPDATE RefundRequest r SET r.status = :toStatus, r.updatedAt = :now " +
           "WHERE r.id = :id AND (r.status = :fromStatus OR r.status = com.inkpulse.entities.enums.RefundStatus.FAILED)")
    int updateStatusSecurely(
            @Param("id") UUID id,
            @Param("fromStatus") RefundStatus fromStatus,
            @Param("toStatus") RefundStatus toStatus,
            @Param("now") LocalDateTime now
    );

    @Query("SELECT r FROM RefundRequest r JOIN r.order o WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:keyword IS NULL OR LOWER(o.orderCode) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))) AND " +
           "(CAST(:startDate AS LocalDateTime) IS NULL OR r.createdAt >= :startDate) AND " +
           "(CAST(:endDate AS LocalDateTime) IS NULL OR r.createdAt <= :endDate)")
    Page<RefundRequest> searchRefunds(
            @Param("status") RefundStatus status,
            @Param("keyword") String keyword,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}
