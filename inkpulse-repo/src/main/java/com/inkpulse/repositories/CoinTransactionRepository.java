package com.inkpulse.repositories;

import com.inkpulse.entities.CoinTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, UUID> {
}
