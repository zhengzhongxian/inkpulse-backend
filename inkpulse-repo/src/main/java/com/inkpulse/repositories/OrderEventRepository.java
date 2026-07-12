package com.inkpulse.repositories;

import com.inkpulse.entities.OrderEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderEventRepository extends JpaRepository<OrderEvent, UUID> {
    List<OrderEvent> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}
