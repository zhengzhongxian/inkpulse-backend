package com.inkpulse.repositories;

import com.inkpulse.entities.FlashSaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface FlashSaleItemRepository extends JpaRepository<FlashSaleItem, UUID>, JpaSpecificationExecutor<FlashSaleItem> {
    List<FlashSaleItem> findByFlashSaleIdAndIdIn(UUID flashSaleId, List<UUID> ids);
    boolean existsByFlashSaleIdAndBookEditionId(UUID flashSaleId, UUID bookEditionId);

    @Query("SELECT fsi FROM FlashSaleItem fsi " +
           "JOIN FETCH fsi.bookEdition " +
           "JOIN fsi.flashSale fs " +
           "WHERE fs.isActive = true AND fs.startDate <= :now AND fs.endDate > :now " +
           "AND fsi.bookEdition.id IN :editionIds")
    List<FlashSaleItem> findActiveByBookEditionIds(
        @Param("editionIds") Collection<UUID> editionIds,
        @Param("now") ZonedDateTime now);
}
