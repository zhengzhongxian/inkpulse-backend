package com.inkpulse.repositories;

import com.inkpulse.entities.FlashSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface FlashSaleRepository extends JpaRepository<FlashSale, UUID>, JpaSpecificationExecutor<FlashSale> {
}
