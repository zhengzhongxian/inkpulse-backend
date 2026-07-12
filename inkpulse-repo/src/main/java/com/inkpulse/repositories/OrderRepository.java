package com.inkpulse.repositories;

import com.inkpulse.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import com.inkpulse.entities.enums.OrderStatus;
import com.inkpulse.entities.enums.PaymentMethod;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByOrderCode(String orderCode);
    boolean existsByOrderCode(String orderCode);
    Page<Order> findByUserId(UUID userId, Pageable pageable);
    Optional<Order> findByIdAndUserId(UUID orderId, UUID userId);

    Page<Order> findByOrderStatus(OrderStatus orderStatus, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.orderStatus = :status AND " +
           "(LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.receiverName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Order> searchOrdersInternal(
            @Param("status") OrderStatus status,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT o FROM Order o WHERE " +
           "LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(o.receiverName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Order> searchOrdersInternalNoStatus(
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT o FROM Order o WHERE " +
           "(:status IS NULL OR o.orderStatus = :status) AND " +
           "(:paymentMethod IS NULL OR o.paymentMethod = :paymentMethod) AND " +
           "(:keyword IS NULL OR " +
           "  LOWER(o.orderCode) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR " +
           "  LOWER(o.receiverName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))) AND " +
           "(CAST(:startDate AS LocalDateTime) IS NULL OR o.createdAt >= :startDate) AND " +
           "(CAST(:endDate AS LocalDateTime) IS NULL OR o.createdAt <= :endDate) AND " +
           "(:minAmount IS NULL OR (o.orderFee + o.shippingFee) >= :minAmount) AND " +
           "(:maxAmount IS NULL OR (o.orderFee + o.shippingFee) <= :maxAmount)")
    Page<Order> searchOrdersInternalAllFilters(
            @Param("status") OrderStatus status,
            @Param("paymentMethod") PaymentMethod paymentMethod,
            @Param("keyword") String keyword,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("minAmount") BigDecimal minAmount,
            @Param("maxAmount") BigDecimal maxAmount,
            Pageable pageable);
}
