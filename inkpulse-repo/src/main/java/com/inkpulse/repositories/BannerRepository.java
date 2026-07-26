package com.inkpulse.repositories;

import com.inkpulse.entities.Banner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BannerRepository extends JpaRepository<Banner, UUID> {

    @Query("SELECT b FROM Banner b WHERE b.isActive = true ORDER BY b.displayOrder ASC, b.createdAt DESC")
    List<Banner> findAllActiveBanners();

    @Query("SELECT b FROM Banner b WHERE (:keyword IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:isActive IS NULL OR b.isActive = :isActive) ORDER BY b.displayOrder ASC, b.createdAt DESC")
    Page<Banner> findPagedBanners(@Param("keyword") String keyword, @Param("isActive") Boolean isActive, Pageable pageable);
}
