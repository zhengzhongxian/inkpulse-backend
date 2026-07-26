package com.inkpulse.repositories;

import com.inkpulse.entities.BannerEdition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BannerEditionRepository extends JpaRepository<BannerEdition, UUID> {

    @Query("SELECT be FROM BannerEdition be JOIN FETCH be.bookEdition e JOIN FETCH e.book b WHERE be.banner.id = :bannerId ORDER BY be.displayOrder ASC")
    List<BannerEdition> findByBannerIdWithEditionDetails(@Param("bannerId") UUID bannerId);

    @Modifying
    @Query("DELETE FROM BannerEdition be WHERE be.banner.id = :bannerId")
    void deleteByBannerId(@Param("bannerId") UUID bannerId);
}
