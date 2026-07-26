package com.inkpulse.features.banner.handlers;

import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.Banner;
import com.inkpulse.entities.BannerEdition;
import com.inkpulse.features.banner.queries.GetPagedBannersQuery;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.response.banner.BannerResponse;
import com.inkpulse.repositories.BannerEditionRepository;
import com.inkpulse.repositories.BannerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetPagedBannersQueryHandler implements Query.QueryHandler<GetPagedBannersQuery, PagedList<BannerResponse>> {

    private final BannerRepository bannerRepository;
    private final BannerEditionRepository bannerEditionRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedList<BannerResponse> handle(GetPagedBannersQuery query) {
        log.info("Handling GetPagedBannersQuery pageNumber={}, pageSize={}, keyword={}",
                query.getPageNumber(), query.getPageSize(), query.getSearchKeyword());

        Pageable pageable = query.toPageable();
        Page<Banner> bannerPage = bannerRepository.findPagedBanners(
                query.getSearchKeyword(),
                query.getIsActive(),
                pageable
        );

        return PagedList.fromPage(bannerPage, banner -> {
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

            return BannerResponse.builder()
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
                .build();
        });
    }
}
