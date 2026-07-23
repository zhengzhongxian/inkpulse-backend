package com.inkpulse.features.flashsale.services;

import com.inkpulse.cache.ICacheService;
import com.inkpulse.constants.KeyConstants;
import com.inkpulse.entities.FlashSaleItem;
import com.inkpulse.repositories.FlashSaleItemRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActiveFlashSaleLookupService {

    private final FlashSaleItemRepository flashSaleItemRepository;
    private final ICacheService cacheService;

    @Data
    @Builder
    public static class FlashSaleItemInfo {
        private UUID flashSaleItemId;
        private BigDecimal discountAmount;
        private BigDecimal originalPrice;
        private BigDecimal flashSalePrice;
        private int stock;
    }

    public Map<UUID, FlashSaleItemInfo> getActiveFlashSalesByEditionIds(Collection<UUID> editionIds) {
        if (editionIds == null || editionIds.isEmpty()) {
            return Collections.emptyMap();
        }

        ZonedDateTime now = ZonedDateTime.now();
        List<FlashSaleItem> activeItems = flashSaleItemRepository.findActiveByBookEditionIds(editionIds, now);

        Map<UUID, FlashSaleItemInfo> result = new HashMap<>();
        for (FlashSaleItem item : activeItems) {
            UUID editionId = item.getBookEdition().getId();
            int currentStock = item.getFlashSaleStock() - item.getSoldCount();

            try {
                String redisStockStr = cacheService.hashGet(KeyConstants.SECTION_FLASHSALE_STOCK,
                        item.getId().toString());
                if (redisStockStr != null) {
                    currentStock = Math.max(0, Integer.parseInt(redisStockStr));
                }
            } catch (Exception e) {
                log.error("Failed to fetch Redis stock for flash sale item ID: {}", item.getId(), e);
            }

            // Only apply flash sale if there is remaining stock
            if (currentStock > 0) {
                BigDecimal catalogPrice = item.getBookEdition().getPrice();
                if (catalogPrice == null) continue;
                BigDecimal dbOldPrice = item.getBookEdition().getOldPrice();
                log.info("=== ActiveFlashSaleLookup DEBUG === editionId: {}, catalogPrice: {}, dbOldPrice: {}, discountAmount: {}", editionId, catalogPrice, dbOldPrice, item.getDiscountAmount());
                BigDecimal originalPrice = dbOldPrice != null ? dbOldPrice : catalogPrice;
                BigDecimal flashSalePrice = catalogPrice.subtract(item.getDiscountAmount());
                if (flashSalePrice.compareTo(BigDecimal.ZERO) < 0) {
                    flashSalePrice = BigDecimal.ZERO;
                }

                result.put(editionId, FlashSaleItemInfo.builder()
                        .flashSaleItemId(item.getId())
                        .discountAmount(item.getDiscountAmount())
                        .originalPrice(originalPrice)
                        .flashSalePrice(flashSalePrice)
                        .stock(currentStock)
                        .build());
            }
        }
        return result;
    }
}
