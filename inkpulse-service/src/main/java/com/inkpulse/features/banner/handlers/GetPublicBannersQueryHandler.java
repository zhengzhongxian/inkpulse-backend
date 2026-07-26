package com.inkpulse.features.banner.handlers;

import com.inkpulse.cache.ICacheService;
import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.Banner;
import com.inkpulse.entities.BannerEdition;
import com.inkpulse.features.banner.queries.GetPublicBannersQuery;
import com.inkpulse.models.response.banner.BannerResponse;
import com.inkpulse.repositories.BannerEditionRepository;
import com.inkpulse.repositories.BannerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetPublicBannersQueryHandler implements Query.QueryHandler<GetPublicBannersQuery, List<BannerResponse>> {

    private final BannerRepository bannerRepository;
    private final BannerEditionRepository bannerEditionRepository;
    private final ICacheService cacheService;

    private static final String PUBLIC_BANNERS_CACHE_KEY = "redis:banners:public_active";

    @Override
    @Transactional(readOnly = true)
    public List<BannerResponse> handle(GetPublicBannersQuery query) {
        log.info("Handling GetPublicBannersQuery with Cache-Aside pattern");

        // 1. Read from Redis Cache
        try {
            BannerResponse[] cached = cacheService.get(PUBLIC_BANNERS_CACHE_KEY, BannerResponse[].class);
            if (cached != null && cached.length > 0) {
                log.debug("Cache HIT for public active banners");
                return List.of(cached);
            }
        } catch (Exception e) {
            log.error("Failed to read public banners from cache", e);
        }

        log.debug("Cache MISS for public active banners. Querying DB...");

        // 2. Query DB
        List<Banner> banners = bannerRepository.findAllActiveBanners();
        List<BannerResponse> responses = new ArrayList<>();

        for (Banner banner : banners) {
            List<BannerEdition> editions = bannerEditionRepository.findByBannerIdWithEditionDetails(banner.getId());
            List<BannerResponse.BannerEditionItemResponse> editionResponses = editions.stream()
                .map(be -> BannerResponse.BannerEditionItemResponse.builder()
                    .editionId(be.getBookEdition().getId())
                    .bookId(be.getBookEdition().getBook().getId())
                    .bookTitle(be.getBookEdition().getBook().getTitle())
                    .isbn(be.getBookEdition().getIsbn())
                    .price(be.getBookEdition().getPrice() != null ? be.getBookEdition().getPrice().toString() : null)
                    .oldPrice(be.getBookEdition().getOldPrice() != null ? be.getBookEdition().getOldPrice().toString() : null)
                    .coverUrl(be.getBookEdition().getThumbnailUrl())
                    .displayOrder(be.getDisplayOrder())
                    .build())
                .toList();

            responses.add(BannerResponse.builder()
                .bannerId(banner.getId())
                .title(banner.getTitle())
                .subtitle(banner.getSubtitle())
                .imageUrl(banner.getImageUrl())
                .iconUrl(banner.getIconUrl())
                .linkUrl(banner.getLinkUrl())
                .displayOrder(banner.getDisplayOrder())
                .isActive(banner.getIsActive())
                .startDate(banner.getStartDate())
                .endDate(banner.getEndDate())
                .createdAt(banner.getCreatedAt())
                .updatedAt(banner.getUpdatedAt())
                .editions(editionResponses)
                .build());
        }

        // 3. Write back to Redis Cache (TTL 30 mins)
        try {
            if (!responses.isEmpty()) {
                cacheService.set(PUBLIC_BANNERS_CACHE_KEY, responses.toArray(new BannerResponse[0]), Duration.ofMinutes(30));
            }
        } catch (Exception e) {
            log.error("Failed to write public banners to cache", e);
        }

        return responses;
    }
}
