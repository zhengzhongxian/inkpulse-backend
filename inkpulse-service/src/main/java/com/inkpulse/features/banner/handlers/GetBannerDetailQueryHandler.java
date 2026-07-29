package com.inkpulse.features.banner.handlers;

import com.inkpulse.cache.SectionCacheService;
import com.inkpulse.constants.message.BannerMessageConstants;
import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.Banner;
import com.inkpulse.entities.BannerEdition;
import com.inkpulse.features.banner.dto.BannerDetailCacheDto;
import com.inkpulse.features.banner.queries.GetBannerDetailQuery;
import com.inkpulse.models.response.banner.BannerResponse;
import com.inkpulse.repositories.BannerEditionRepository;
import com.inkpulse.repositories.BannerRepository;
import com.inkpulse.corehelpers.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import com.inkpulse.constants.KeyConstants;
import com.inkpulse.corehelpers.UrlHelper;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetBannerDetailQueryHandler implements Query.QueryHandler<GetBannerDetailQuery, BannerResponse> {

    private final BannerRepository bannerRepository;
    private final BannerEditionRepository bannerEditionRepository;
    private final SectionCacheService sectionCache;

    @Value("${" + KeyConstants.STORAGE_PUBLIC_URL + ":}")
    private String publicUrl;

    @Value("${" + KeyConstants.MINIO_USE_SSL + ":false}")
    private boolean useSsl;

    @Override
    @Transactional(readOnly = true)
    public BannerResponse handle(GetBannerDetailQuery query) {
        String bannerIdStr = query.getBannerId().toString();
        log.info("Handling GetBannerDetailQuery for banner ID: {}", bannerIdStr);

        // 1. Try reading from Redis Cache (Cache-Aside Pattern)
        try {
            BannerDetailCacheDto cached = sectionCache.get(bannerIdStr, BannerDetailCacheDto.class);
            if (cached != null) {
                log.debug("Cache HIT for banner detail: {}", bannerIdStr);
                return toResponse(cached);
            }
        } catch (Exception e) {
            log.error("Failed to read banner detail from cache: {}", bannerIdStr, e);
        }

        log.debug("Cache MISS for banner detail: {}. Fetching from DB...", bannerIdStr);

        // 2. Fetch from DB
        Banner banner = bannerRepository.findById(query.getBannerId())
                .orElseThrow(() -> new ResourceNotFoundException(BannerMessageConstants.BANNER_NOT_FOUND));

        List<BannerEdition> editions = bannerEditionRepository.findByBannerIdWithEditionDetails(banner.getId());

        List<BannerResponse.BannerEditionItemResponse> editionResponses = editions.stream()
                .map(be -> BannerResponse.BannerEditionItemResponse.builder()
                        .editionId(be.getBookEdition().getId())
                        .bookId(be.getBookEdition().getBook().getId())
                        .bookTitle(be.getBookEdition().getBook().getTitle())
                        .isbn(be.getBookEdition().getIsbn())
                        .price(be.getBookEdition().getPrice() != null ? be.getBookEdition().getPrice().toString() : null)
                        .oldPrice(be.getBookEdition().getOldPrice() != null ? be.getBookEdition().getOldPrice().toString() : null)
                        .coverUrl(UrlHelper.buildAbsoluteUrl(publicUrl, be.getBookEdition().getThumbnailUrl(), useSsl))
                        .displayOrder(be.getDisplayOrder())
                        .build())
                .toList();

        BannerResponse response = BannerResponse.builder()
                .bannerId(banner.getId())
                .title(banner.getTitle())
                .subtitle(banner.getSubtitle())
                .imageUrl(UrlHelper.buildAbsoluteUrl(publicUrl, banner.getImageUrl(), useSsl))
                .iconUrl(UrlHelper.buildAbsoluteUrl(publicUrl, banner.getIconUrl(), useSsl))
                .linkUrl(banner.getLinkUrl())
                .displayOrder(banner.getDisplayOrder())
                .isActive(banner.getIsActive())
                .startDate(banner.getStartDate())
                .endDate(banner.getEndDate())
                .createdAt(banner.getCreatedAt())
                .updatedAt(banner.getUpdatedAt())
                .editions(editionResponses)
                .build();

        // 3. Write back to Redis Cache
        try {
            List<BannerDetailCacheDto.BannerEditionCacheDto> cacheEditions = editionResponses.stream()
                    .map(e -> new BannerDetailCacheDto.BannerEditionCacheDto(
                            e.getEditionId() != null ? e.getEditionId().toString() : null,
                            e.getBookId() != null ? e.getBookId().toString() : null,
                            e.getBookTitle(),
                            e.getIsbn(),
                            e.getPrice(),
                            e.getOldPrice(),
                            e.getCoverUrl(),
                            e.getDisplayOrder() != null ? e.getDisplayOrder() : 0
                    ))
                    .toList();

            BannerDetailCacheDto cacheDto = new BannerDetailCacheDto(
                    bannerIdStr,
                    banner.getTitle(),
                    banner.getSubtitle(),
                    banner.getImageUrl(),
                    banner.getIconUrl(),
                    banner.getLinkUrl(),
                    banner.getDisplayOrder() != null ? banner.getDisplayOrder() : 0,
                    Boolean.TRUE.equals(banner.getIsActive()),
                    banner.getStartDate() != null ? banner.getStartDate().toString() : null,
                    banner.getEndDate() != null ? banner.getEndDate().toString() : null,
                    cacheEditions
            );
            sectionCache.set(cacheDto);
        } catch (Exception e) {
            log.error("Failed to put banner detail to cache: {}", bannerIdStr, e);
        }

        return response;
    }

    private BannerResponse toResponse(BannerDetailCacheDto cached) {
        List<BannerResponse.BannerEditionItemResponse> editions = cached.editions() != null
                ? cached.editions().stream()
                .map(e -> BannerResponse.BannerEditionItemResponse.builder()
                        .editionId(e.editionId() != null ? java.util.UUID.fromString(e.editionId()) : null)
                        .bookId(e.bookId() != null ? java.util.UUID.fromString(e.bookId()) : null)
                        .bookTitle(e.bookTitle())
                        .isbn(e.isbn())
                        .price(e.price())
                        .oldPrice(e.oldPrice())
                        .coverUrl(UrlHelper.buildAbsoluteUrl(publicUrl, e.coverUrl(), useSsl))
                        .displayOrder(e.displayOrder())
                        .build())
                .toList()
                : List.of();

        return BannerResponse.builder()
                .bannerId(java.util.UUID.fromString(cached.bannerId()))
                .title(cached.title())
                .subtitle(cached.subtitle())
                .imageUrl(UrlHelper.buildAbsoluteUrl(publicUrl, cached.imageUrl(), useSsl))
                .iconUrl(UrlHelper.buildAbsoluteUrl(publicUrl, cached.iconUrl(), useSsl))
                .linkUrl(cached.linkUrl())
                .displayOrder(cached.displayOrder())
                .isActive(cached.isActive())
                .editions(editions)
                .build();
    }
}
