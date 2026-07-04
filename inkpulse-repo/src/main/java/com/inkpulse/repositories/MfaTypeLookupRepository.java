package com.inkpulse.repositories;

import com.inkpulse.entities.MfaTypeLookup;
import com.inkpulse.entities.enums.MfaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MfaTypeLookupRepository extends JpaRepository<MfaTypeLookup, UUID> {
    Optional<MfaTypeLookup> findByTypeName(MfaType typeName);
}
