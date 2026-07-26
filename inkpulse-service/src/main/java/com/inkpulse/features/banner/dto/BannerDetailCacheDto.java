package com.inkpulse.features.banner.dto;

import com.inkpulse.cache.CacheSection;
import com.inkpulse.cache.Cacheable;
import com.inkpulse.constants.KeyConstants;

import java.util.List;

@CacheSection(KeyConstants.SECTION_BANNERS)
public record BannerDetailCacheDto(
    String bannerId,
    String title,
    String subtitle,
    String imageUrl,
    String iconUrl,
    String linkUrl,
    int displayOrder,
    boolean isActive,
    String startDate,
    String endDate,
    List<BannerEditionCacheDto> editions
) implements Cacheable {

    @Override
    public String cacheId() {
        return bannerId;
    }

    public record BannerEditionCacheDto(
        String editionId,
        String bookId,
        String bookTitle,
        String isbn,
        String price,
        String oldPrice,
        String coverUrl,
        int displayOrder
    ) {}
}
