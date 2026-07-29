package com.inkpulse.features.banner.handlers;

import com.inkpulse.cache.ICacheService;
import com.inkpulse.constants.message.BannerMessageConstants;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.Banner;
import com.inkpulse.entities.BannerEdition;
import com.inkpulse.entities.BookEdition;
import com.inkpulse.features.banner.commands.UpdateBannerCommand;
import com.inkpulse.models.request.banner.UpdateBannerRequest;
import com.inkpulse.models.response.banner.BannerResponse;
import com.inkpulse.repositories.BannerEditionRepository;
import com.inkpulse.repositories.BannerRepository;
import com.inkpulse.repositories.BookEditionRepository;
import com.inkpulse.service.minio.IMinioService;
import com.inkpulse.service.minio.MinioFileInfo;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.corehelpers.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateBannerHandler implements Command.CommandHandler<UpdateBannerCommand, BannerResponse> {

    private final BannerRepository bannerRepository;
    private final BannerEditionRepository bannerEditionRepository;
    private final BookEditionRepository bookEditionRepository;
    private final IMinioService minioService;
    private final ICacheService cacheService;

    @Override
    @Transactional
    public BannerResponse handle(UpdateBannerCommand command) {
        UUID bannerId = command.getBannerId();
        UpdateBannerRequest req = command.getRequest();
        log.info("Handling UpdateBannerCommand for ID: {}", bannerId);

        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new ResourceNotFoundException(BannerMessageConstants.BANNER_NOT_FOUND));

        banner.setTitle(req.getTitle());
        banner.setSubtitle(req.getSubtitle());
        if (req.getLinkUrl() != null) banner.setLinkUrl(req.getLinkUrl());
        if (req.getDisplayOrder() != null) banner.setDisplayOrder(req.getDisplayOrder());
        if (req.getIsActive() != null) banner.setIsActive(req.getIsActive());
        if (req.getStartDate() != null) banner.setStartDate(req.getStartDate());
        if (req.getEndDate() != null) banner.setEndDate(req.getEndDate());

        // Update image via MinIO if file uploaded
        if (command.getImageFile() != null) {
            try {
                String objectName = "banners/" + bannerId + "/image_" + UUID.randomUUID() + "_" + command.getImageFile().getFileName();
                minioService.uploadFile(
                        command.getImageFile().getInputStream(),
                        command.getImageFile().getFileName(),
                        command.getImageFile().getContentType(),
                        command.getImageFile().getFileSize(),
                        objectName,
                        Map.of("bannerId", bannerId.toString())
                );
                banner.setImageUrl("books/" + objectName);
            } catch (Exception e) {
                log.error("Failed to update banner image in MinIO for ID {}", bannerId, e);
                throw new BusinessValidationException("Lỗi khi tải ảnh banner mới lên MinIO.");
            }
        } else if (req.getImageUrl() != null && !req.getImageUrl().trim().isEmpty()) {
            banner.setImageUrl(req.getImageUrl());
        }

        // Update icon via MinIO if file uploaded
        if (command.getIconFile() != null) {
            try {
                String objectName = "banners/" + bannerId + "/icon_" + UUID.randomUUID() + "_" + command.getIconFile().getFileName();
                minioService.uploadFile(
                        command.getIconFile().getInputStream(),
                        command.getIconFile().getFileName(),
                        command.getIconFile().getContentType(),
                        command.getIconFile().getFileSize(),
                        objectName,
                        Map.of("bannerId", bannerId.toString())
                );
                banner.setIconUrl("books/" + objectName);
            } catch (Exception e) {
                log.error("Failed to update banner icon in MinIO for ID {}", bannerId, e);
            }
        } else if (req.getIconUrl() != null) {
            banner.setIconUrl(req.getIconUrl());
        }

        // Update linked Book Editions if provided
        if (req.getEditionIds() != null) {
            bannerEditionRepository.deleteByBannerId(bannerId);
            List<BannerEdition> newBannerEditions = new ArrayList<>();
            int order = 0;
            for (UUID editionId : req.getEditionIds()) {
                BookEdition edition = bookEditionRepository.findById(editionId).orElse(null);
                if (edition != null) {
                    BannerEdition be = BannerEdition.builder()
                            .banner(banner)
                            .bookEdition(edition)
                            .displayOrder(order++)
                            .build();
                    newBannerEditions.add(be);
                }
            }
            bannerEditionRepository.saveAll(newBannerEditions);
            banner.setBannerEditions(newBannerEditions);
        }

        bannerRepository.save(banner);

        // Evict Redis Cache
        evictBannerCaches(bannerId);

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

    private void evictBannerCaches(UUID bannerId) {
        try {
            cacheService.remove("redis:banners:public_active");
            cacheService.remove("redis:banners:" + bannerId);
        } catch (Exception e) {
            log.error("Failed to evict banner caches", e);
        }
    }
}
