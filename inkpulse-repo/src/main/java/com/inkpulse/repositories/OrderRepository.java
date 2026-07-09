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
import com.inkpulse.entities.enums.OrderStatus;

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
}
