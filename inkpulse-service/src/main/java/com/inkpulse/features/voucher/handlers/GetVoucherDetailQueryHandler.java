package com.inkpulse.features.voucher.handlers;

import com.inkpulse.cache.SectionCacheService;
import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.*;
import com.inkpulse.entities.enums.VoucherTargetType;
import com.inkpulse.features.voucher.dto.VoucherDetailCacheDto;
import com.inkpulse.features.voucher.queries.GetVoucherDetailQuery;
import com.inkpulse.models.response.voucher.VoucherDetailResponse;
import com.inkpulse.repositories.*;
import com.inkpulse.corehelpers.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetVoucherDetailQueryHandler implements Query.QueryHandler<GetVoucherDetailQuery, VoucherDetailResponse> {

    private final VoucherRepository voucherRepository;
    private final VoucherBookRepository voucherBookRepository;
    private final VoucherCategoryRepository voucherCategoryRepository;
    private final VoucherEditionRepository voucherEditionRepository;
    private final SectionCacheService sectionCache;

    @Override
    @Transactional(readOnly = true)
    public VoucherDetailResponse handle(GetVoucherDetailQuery query) {
        String voucherIdStr = query.getVoucherId().toString();
        log.info("Handling GetVoucherDetailQuery for ID: {}", voucherIdStr);

        // 1. Try to read from cache (Cache-Aside Pattern)
        VoucherDetailCacheDto cachedDto = null;
        try {
            cachedDto = sectionCache.get(voucherIdStr, VoucherDetailCacheDto.class);
        } catch (Exception e) {
            log.error("Failed to read voucher detail from cache: {}", voucherIdStr, e);
        }

        if (cachedDto != null) {
            log.debug("Cache hit for voucher detail: {}", voucherIdStr);
            return toResponse(cachedDto);
        }

        log.debug("Cache miss for voucher detail: {}. Fetching from DB...", voucherIdStr);

        // 2. Fetch from DB
        Voucher voucher = voucherRepository.findById(query.getVoucherId())
                .orElseThrow(() -> new ResourceNotFoundException("Voucher", "id", query.getVoucherId()));

        List<VoucherDetailCacheDto.VoucherTargetItemCacheDto> targetItems = new ArrayList<>();
        VoucherTargetType targetType = voucher.getTargetType();

        if (targetType != VoucherTargetType.ALL) {
            switch (targetType) {
                case CATEGORY -> {
                    List<VoucherCategory> mappings = voucherCategoryRepository.findByVoucherId(voucher.getId());
                    for (VoucherCategory mapping : mappings) {
                        if (mapping.getCategory() != null) {
                            targetItems.add(new VoucherDetailCacheDto.VoucherTargetItemCacheDto(
                                    mapping.getCategory().getId().toString(),
                                    mapping.getCategory().getName()
                            ));
                        }
                    }
                }
                case BOOK -> {
                    List<VoucherBook> mappings = voucherBookRepository.findByVoucherId(voucher.getId());
                    for (VoucherBook mapping : mappings) {
                        if (mapping.getBook() != null) {
                            targetItems.add(new VoucherDetailCacheDto.VoucherTargetItemCacheDto(
                                    mapping.getBook().getId().toString(),
                                    mapping.getBook().getTitle()
                            ));
                        }
                    }
                }
                case EDITION -> {
                    List<VoucherEdition> mappings = voucherEditionRepository.findByVoucherId(voucher.getId());
                    for (VoucherEdition mapping : mappings) {
                        if (mapping.getBookEdition() != null) {
                            BookEdition edition = mapping.getBookEdition();
                            String editionName = edition.getBook().getTitle() + " - " + 
                                    (edition.getCoverType() != null ? edition.getCoverType().name() : "Standard") + 
                                    " (ISBN: " + edition.getIsbn() + ")";
                            targetItems.add(new VoucherDetailCacheDto.VoucherTargetItemCacheDto(
                                    edition.getId().toString(),
                                    editionName
                            ));
                        }
                    }
                }
            }
        }

        // Build Cache DTO
        VoucherDetailCacheDto cacheDto = new VoucherDetailCacheDto(
                voucher.getId().toString(),
                voucher.getStartDate().toString(),
                voucher.getEndDate().toString(),
                voucher.getVoucherCode(),
                voucher.getDescription(),
                voucher.getDiscountType().name(),
                voucher.getDiscountValue().stripTrailingZeros().toPlainString(),
                voucher.getMinOrderValue().stripTrailingZeros().toPlainString(),
                voucher.getMaxUses(),
                voucher.getUsedCount(),
                voucher.getMaxUsesPerUser(),
                voucher.getIsActive(),
                voucher.getCoinCost(),
                voucher.getTargetType().name(),
                voucher.getMaxDiscountAmount() != null ? voucher.getMaxDiscountAmount().stripTrailingZeros().toPlainString() : null,
                voucher.getCreatedAt().toString(),
                targetItems
        );

        // 3. Save to Cache
        try {
            sectionCache.set(cacheDto);
            log.debug("Saved voucher detail to cache: {}", voucherIdStr);
        } catch (Exception e) {
            log.error("Failed to save voucher detail to cache: {}", voucherIdStr, e);
        }

        return toResponse(cacheDto);
    }

    private VoucherDetailResponse toResponse(VoucherDetailCacheDto dto) {
        List<VoucherDetailResponse.VoucherTargetItemResponse> items = new ArrayList<>();
        if (dto.targetItems() != null) {
            for (VoucherDetailCacheDto.VoucherTargetItemCacheDto item : dto.targetItems()) {
                items.add(new VoucherDetailResponse.VoucherTargetItemResponse(
                        item.id(),
                        item.name()
                ));
            }
        }

        return new VoucherDetailResponse(
                dto.voucherId(),
                dto.startDate(),
                dto.endDate(),
                dto.voucherCode(),
                dto.description(),
                dto.discountType(),
                dto.discountValue(),
                dto.minOrderValue(),
                dto.maxUses(),
                dto.usedCount(),
                dto.maxUsesPerUser(),
                dto.isActive(),
                dto.coinCost(),
                dto.targetType(),
                dto.maxDiscountAmount(),
                dto.createdAt(),
                items
        );
    }
}
