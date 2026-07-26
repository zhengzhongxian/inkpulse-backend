package com.inkpulse.features.banner.handlers;

import com.inkpulse.cache.ICacheService;
import com.inkpulse.constants.message.BannerMessageConstants;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.Banner;
import com.inkpulse.features.banner.commands.DeleteBannerCommand;
import com.inkpulse.repositories.BannerRepository;
import com.inkpulse.corehelpers.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteBannerHandler implements Command.CommandHandler<DeleteBannerCommand, Void> {

    private final BannerRepository bannerRepository;
    private final ICacheService cacheService;

    @Override
    @Transactional
    public Void handle(DeleteBannerCommand command) {
        UUID bannerId = command.getBannerId();
        log.info("Handling DeleteBannerCommand for ID: {}", bannerId);

        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new ResourceNotFoundException(BannerMessageConstants.BANNER_NOT_FOUND));

        banner.setDeleted(true);
        bannerRepository.save(banner);

        // Evict Cache
        try {
            cacheService.remove("redis:banners:public_active");
            cacheService.remove("redis:banners:" + bannerId);
        } catch (Exception e) {
            log.error("Failed to evict banner caches for ID {}", bannerId, e);
        }

        return null;
    }
}
