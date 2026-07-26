package com.inkpulse.features.banner.handlers;

import com.inkpulse.cache.ICacheService;
import com.inkpulse.constants.message.BannerMessageConstants;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.Banner;
import com.inkpulse.entities.BannerEdition;
import com.inkpulse.entities.BookEdition;
import com.inkpulse.features.banner.commands.CreateBannerCommand;
import com.inkpulse.models.request.banner.CreateBannerRequest;
import com.inkpulse.models.response.banner.BannerResponse;
import com.inkpulse.repositories.BannerEditionRepository;
import com.inkpulse.repositories.BannerRepository;
import com.inkpulse.repositories.BookEditionRepository;
import com.inkpulse.service.minio.IMinioService;
import com.inkpulse.service.minio.MinioFileInfo;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
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
public class CreateBannerHandler implements Command.CommandHandler<CreateBannerCommand, BannerResponse> {

    private final BannerRepository bannerRepository;
    private final BannerEditionRepository bannerEditionRepository;
    private final BookEditionRepository bookEditionRepository;
    private final IMinioService minioService;
    private final ICacheService cacheService;

    @Override
    @Transactional
    public BannerResponse handle(CreateBannerCommand command) {
        CreateBannerRequest req = command.getRequest();
        log.info("Handling CreateBannerCommand for title: {}", req.getTitle());

        // 1. Create & Persist initial Banner entity (DO NOT set ID manually)
        Banner banner = Banner.builder()
                .title(req.getTitle())
                .subtitle(req.getSubtitle())
                .imageUrl(req.getImageUrl() != null ? req.getImageUrl() : "")
                .iconUrl(req.getIconUrl())
                .linkUrl(req.getLinkUrl())
                .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0)
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .build();

        // Save first to get generated ID from Hibernate
        banner = bannerRepository.save(banner);
        UUID bannerId = banner.getId();

        // 2. Process image upload via MinIO if file provided
        if (command.getImageFile() != null) {
            try {
                String objectName = "banners/" + bannerId + "/image_" + UUID.randomUUID() + "_" + command.getImageFile().getFileName();
                MinioFileInfo fileInfo = minioService.uploadFile(
                        command.getImageFile().getInputStream(),
                        command.getImageFile().getFileName(),
                        command.getImageFile().getContentType(),
                        command.getImageFile().getFileSize(),
                        objectName,
                        Map.of("bannerId", bannerId.toString())
                );
                banner.setImageUrl(fileInfo.getUrl());
            } catch (Exception e) {
                log.error("Failed to upload banner image to MinIO for ID {}", bannerId, e);
                throw new BusinessValidationException("Lỗi khi tải ảnh banner lên hệ thống lưu trữ MinIO.");
            }
        }

        // Process icon upload via MinIO if file provided
        if (command.getIconFile() != null) {
            try {
                String objectName = "banners/" + bannerId + "/icon_" + UUID.randomUUID() + "_" + command.getIconFile().getFileName();
                MinioFileInfo fileInfo = minioService.uploadFile(
                        command.getIconFile().getInputStream(),
                        command.getIconFile().getFileName(),
                        command.getIconFile().getContentType(),
                        command.getIconFile().getFileSize(),
                        objectName,
                        Map.of("bannerId", bannerId.toString())
                );
                banner.setIconUrl(fileInfo.getUrl());
            } catch (Exception e) {
                log.error("Failed to upload banner icon to MinIO for ID {}", bannerId, e);
            }
        }

        // Validate image URL
        if (banner.getImageUrl() == null || banner.getImageUrl().trim().isEmpty()) {
            throw new BusinessValidationException(BannerMessageConstants.BANNER_IMAGE_REQUIRED);
        }

        // 3. Link Book Editions if provided
        List<BannerEdition> bannerEditions = new ArrayList<>();
        if (req.getEditionIds() != null && !req.getEditionIds().isEmpty()) {
            int order = 0;
            for (UUID editionId : req.getEditionIds()) {
                BookEdition edition = bookEditionRepository.findById(editionId).orElse(null);
                if (edition != null) {
                    BannerEdition be = BannerEdition.builder()
                            .banner(banner)
                            .bookEdition(edition)
                            .displayOrder(order++)
                            .build();
                    bannerEditions.add(be);
                }
            }
            bannerEditionRepository.saveAll(bannerEditions);
        }

        banner.setBannerEditions(bannerEditions);
        bannerRepository.save(banner);

        // 4. Evict Redis Cache
        evictBannerCaches(bannerId);

        return BannerResponse.builder()
                .bannerId(bannerId)
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
