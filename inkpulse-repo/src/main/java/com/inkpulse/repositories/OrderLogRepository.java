package com.inkpulse.repositories;

import com.inkpulse.entities.OrderLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface OrderLogRepository extends JpaRepository<OrderLog, UUID> {
}
