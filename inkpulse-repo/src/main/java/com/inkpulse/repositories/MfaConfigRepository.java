package com.inkpulse.repositories;

import com.inkpulse.entities.MfaConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface MfaConfigRepository extends JpaRepository<MfaConfig, UUID> {
    List<MfaConfig> findAllByUserId(UUID userId);
}
