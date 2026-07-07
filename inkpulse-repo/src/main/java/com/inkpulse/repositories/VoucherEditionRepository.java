package com.inkpulse.repositories;

import com.inkpulse.entities.VoucherEdition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface VoucherEditionRepository extends JpaRepository<VoucherEdition, UUID> {
}
