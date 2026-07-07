package com.inkpulse.repositories;

import com.inkpulse.entities.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, UUID> {
    List<UserAddress> findByUserId(UUID userId);
    Optional<UserAddress> findFirstByUserIdOrderByLastUsedAtDesc(UUID userId);
}
