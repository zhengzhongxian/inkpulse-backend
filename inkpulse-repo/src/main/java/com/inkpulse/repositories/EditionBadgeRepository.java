package com.inkpulse.repositories;

import com.inkpulse.entities.EditionBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface EditionBadgeRepository extends JpaRepository<EditionBadge, UUID> {

    @Query("SELECT eb FROM EditionBadge eb WHERE eb.badge.id = :badgeId")
    List<EditionBadge> findByBadgeId(@Param("badgeId") UUID badgeId);

    @Modifying
    @Query(value = "DELETE FROM editions_badges WHERE edition_id = :editionId", nativeQuery = true)
    void deleteByEditionIdPhysical(@Param("editionId") UUID editionId);
}