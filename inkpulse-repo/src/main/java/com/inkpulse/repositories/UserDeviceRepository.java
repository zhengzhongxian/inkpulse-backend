package com.inkpulse.repositories;

import com.inkpulse.entities.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, UUID> {
    Optional<UserDevice> findByUserIdAndId(UUID userId, UUID deviceId);
    List<UserDevice> findAllByUserId(UUID userId);
}
