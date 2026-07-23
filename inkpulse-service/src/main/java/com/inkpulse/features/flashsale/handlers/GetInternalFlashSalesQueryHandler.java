package com.inkpulse.features.flashsale.handlers;

import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.FlashSale;
import com.inkpulse.features.flashsale.queries.GetInternalFlashSalesQuery;
import com.inkpulse.features.flashsale.specifications.FlashSaleSpecifications;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.response.flashsale.FlashSaleResponse;
import com.inkpulse.repositories.FlashSaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetInternalFlashSalesQueryHandler implements Query.QueryHandler<GetInternalFlashSalesQuery, PagedList<FlashSaleResponse>> {

    private final FlashSaleRepository flashSaleRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedList<FlashSaleResponse> handle(GetInternalFlashSalesQuery query) {
        log.info("Handling GetInternalFlashSalesQuery");

        Specification<FlashSale> spec = Specification.where(FlashSaleSpecifications.hasKeyword(query.getSearchKeyword()))
                .and(FlashSaleSpecifications.isActive(query.getIsActive()))
                .and(FlashSaleSpecifications.startDateBetween(query.getStartDateFrom(), query.getStartDateTo()))
                .and(FlashSaleSpecifications.endDateBetween(query.getEndDateFrom(), query.getEndDateTo()))
                .and(FlashSaleSpecifications.createdBetween(query.getCreatedFrom(), query.getCreatedTo()))
                .and(FlashSaleSpecifications.stockBetween(query.getMinStock(), query.getMaxStock()))
                .and(FlashSaleSpecifications.discountAmountBetween(query.getMinDiscount(), query.getMaxDiscount()));

        Page<FlashSale> page = flashSaleRepository.findAll(spec, query.toPageable());

        return PagedList.fromPage(page, this::toResponse);
    }

    private FlashSaleResponse toResponse(FlashSale flashSale) {
        return FlashSaleResponse.builder()
                .flashSaleId(flashSale.getId().toString())
                .name(flashSale.getName())
                .itemCount(flashSale.getItems() != null ? flashSale.getItems().size() : 0)
                .isActive(flashSale.getIsActive())
                .startDate(flashSale.getStartDate())
                .endDate(flashSale.getEndDate())
                .createdAt(flashSale.getCreatedAt())
                .build();
    }
}
