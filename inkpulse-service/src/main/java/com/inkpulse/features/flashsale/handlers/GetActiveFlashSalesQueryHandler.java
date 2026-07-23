package com.inkpulse.features.flashsale.handlers;

import com.inkpulse.cache.ICacheService;
import com.inkpulse.constants.KeyConstants;
import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.FlashSale;
import com.inkpulse.features.flashsale.queries.GetActiveFlashSalesQuery;
import com.inkpulse.features.flashsale.specifications.FlashSaleSpecifications;
import com.inkpulse.models.response.flashsale.FlashSaleResponse;
import com.inkpulse.repositories.FlashSaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Sort;

import com.inkpulse.entities.FlashSaleItem;
import com.inkpulse.models.response.flashsale.FlashSaleItemResponse;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetActiveFlashSalesQueryHandler implements Query.QueryHandler<GetActiveFlashSalesQuery, List<FlashSaleItemResponse>> {

    private final FlashSaleRepository flashSaleRepository;
    private final ICacheService cacheService;

    @Override
    @Transactional(readOnly = true)
    public List<FlashSaleItemResponse> handle(GetActiveFlashSalesQuery query) {
        log.info("Handling GetActiveFlashSalesQuery");

        Specification<FlashSale> spec = Specification.where(FlashSaleSpecifications.isActiveAndNotEnded());
        List<FlashSale> flashSales = flashSaleRepository.findAll(spec, Sort.by(Sort.Order.asc("startDate")));

        List<FlashSaleItemResponse> responses = new ArrayList<>();
        for (FlashSale fs : flashSales) {
            if (fs.getItems() == null) continue;
            for (FlashSaleItem item : fs.getItems()) {
                int currentStock = item.getFlashSaleStock() - item.getSoldCount();
                
                // Merge with Redis stock if available
                try {
                    String redisStockStr = cacheService.hashGet(KeyConstants.SECTION_FLASHSALE_STOCK, item.getId().toString());
                    if (redisStockStr != null) {
                        currentStock = Math.max(0, Integer.parseInt(redisStockStr));
                    }
                } catch (Exception e) {
                    log.error("Failed to fetch Redis stock for flash sale item ID: {}", item.getId(), e);
                }

                responses.add(FlashSaleItemResponse.builder()
                        .flashSaleItemId(item.getId().toString())
                        .flashSaleId(fs.getId().toString())
                        .name(fs.getName())
                        .bookEditionId(item.getBookEdition().getId().toString())
                        .bookTitle(item.getBookEdition().getBook() != null ? item.getBookEdition().getBook().getTitle() : null)
                        .editionTitle(item.getBookEdition().getIsbn())
                        .thumbnailUrl(item.getBookEdition().getThumbnailUrl())
                        .originalPrice(item.getBookEdition().getOldPrice() != null ? item.getBookEdition().getOldPrice() : item.getBookEdition().getPrice())
                        .discountAmount(item.getDiscountAmount())
                        .flashSalePrice(item.getBookEdition().getPrice().subtract(item.getDiscountAmount()))
                        .flashSaleStock(currentStock)
                        .soldCount(item.getSoldCount())
                        .startDate(fs.getStartDate())
                        .endDate(fs.getEndDate())
                        .build());
            }
        }

        return responses;
    }
}
