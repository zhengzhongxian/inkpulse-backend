package com.inkpulse.repositories;

import com.inkpulse.entities.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserPermissionRepository extends JpaRepository<UserPermission, UUID> {
    List<UserPermission> findAllByUserId(UUID userId);
}
