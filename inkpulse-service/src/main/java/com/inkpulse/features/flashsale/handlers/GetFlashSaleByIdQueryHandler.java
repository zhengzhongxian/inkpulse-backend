package com.inkpulse.features.flashsale.handlers;

import com.inkpulse.constants.message.FlashSaleMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.FlashSale;
import com.inkpulse.features.flashsale.queries.GetFlashSaleByIdQuery;
import com.inkpulse.models.response.flashsale.FlashSaleDetailResponse;
import com.inkpulse.repositories.FlashSaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.inkpulse.models.response.flashsale.FlashSaleItemResponse;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetFlashSaleByIdQueryHandler implements Query.QueryHandler<GetFlashSaleByIdQuery, FlashSaleDetailResponse> {

    private final FlashSaleRepository flashSaleRepository;

    @Override
    @Transactional(readOnly = true)
    public FlashSaleDetailResponse handle(GetFlashSaleByIdQuery query) {
        log.info("Handling GetFlashSaleByIdQuery for ID: {}", query.getFlashSaleId());

        FlashSale flashSale = flashSaleRepository.findById(query.getFlashSaleId())
                .orElseThrow(() -> new BusinessValidationException(
                        FlashSaleMessageConstants.FLASHSALE_NOT_FOUND,
                        FlashSaleMessageConstants.CODE_FLASHSALE_NOT_FOUND
                ));

        return toDetailResponse(flashSale);
    }

    private FlashSaleDetailResponse toDetailResponse(FlashSale flashSale) {
        List<FlashSaleItemResponse> items = flashSale.getItems().stream()
                .map(item -> FlashSaleItemResponse.builder()
                        .flashSaleItemId(item.getId().toString())
                        .flashSaleId(flashSale.getId().toString())
                        .name(flashSale.getName())
                        .bookEditionId(item.getBookEdition().getId().toString())
                        .bookTitle(item.getBookEdition().getBook() != null ? item.getBookEdition().getBook().getTitle() : null)
                        .editionTitle(item.getBookEdition().getIsbn())
                        .thumbnailUrl(item.getBookEdition().getThumbnailUrl())
                        .originalPrice(item.getBookEdition().getPrice())
                        .discountAmount(item.getDiscountAmount())
                        .flashSalePrice(item.getBookEdition().getPrice().subtract(item.getDiscountAmount()))
                        .flashSaleStock(item.getFlashSaleStock())
                        .soldCount(item.getSoldCount())
                        .startDate(flashSale.getStartDate())
                        .endDate(flashSale.getEndDate())
                        .build())
                .collect(Collectors.toList());

        return FlashSaleDetailResponse.builder()
                .flashSaleId(flashSale.getId().toString())
                .name(flashSale.getName())
                .itemCount(items.size())
                .isActive(flashSale.getIsActive())
                .startDate(flashSale.getStartDate())
                .endDate(flashSale.getEndDate())
                .createdAt(flashSale.getCreatedAt())
                .items(items)
                .build();
    }
}
