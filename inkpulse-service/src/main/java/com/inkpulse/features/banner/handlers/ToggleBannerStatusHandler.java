package com.inkpulse.features.banner.handlers;

import com.inkpulse.cache.ICacheService;
import com.inkpulse.constants.message.BannerMessageConstants;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.Banner;
import com.inkpulse.features.banner.commands.ToggleBannerStatusCommand;
import com.inkpulse.models.response.banner.BannerResponse;
import com.inkpulse.repositories.BannerRepository;
import com.inkpulse.corehelpers.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ToggleBannerStatusHandler implements Command.CommandHandler<ToggleBannerStatusCommand, BannerResponse> {

    private final BannerRepository bannerRepository;
    private final ICacheService cacheService;

    @Override
    @Transactional
    public BannerResponse handle(ToggleBannerStatusCommand command) {
        UUID bannerId = command.getBannerId();
        log.info("Handling ToggleBannerStatusCommand for ID: {}", bannerId);

        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new ResourceNotFoundException(BannerMessageConstants.BANNER_NOT_FOUND));

        banner.setIsActive(!Boolean.TRUE.equals(banner.getIsActive()));
        bannerRepository.save(banner);

        // Evict Cache
        try {
            cacheService.remove("redis:banners:public_active");
            cacheService.remove("redis:banners:" + bannerId);
        } catch (Exception e) {
            log.error("Failed to evict banner caches for ID {}", bannerId, e);
        }

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
                .build();
    }
}
