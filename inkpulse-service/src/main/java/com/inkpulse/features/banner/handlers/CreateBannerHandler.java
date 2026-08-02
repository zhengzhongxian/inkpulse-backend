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
import org.springframework.transaction.support.TransactionTemplate;

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
    private final TransactionTemplate transactionTemplate;

    @Override
    public BannerResponse handle(CreateBannerCommand command) {
        CreateBannerRequest req = command.getRequest();
        log.info("Handling CreateBannerCommand for title: {}", req.getTitle());

        UUID tempBannerId = UUID.randomUUID();
        String uploadedImageUrl = req.getImageUrl() != null ? req.getImageUrl() : "";
        String uploadedIconUrl = req.getIconUrl();
        String imageObjectName = null;
        String iconObjectName = null;

        // Process image upload via MinIO if file provided (outside DB transaction)
        if (command.getImageFile() != null) {
            try {
                imageObjectName = "banners/" + tempBannerId + "/image_" + UUID.randomUUID() + "_" + command.getImageFile().getFileName();
                minioService.uploadFile(
                        command.getImageFile().getInputStream(),
                        command.getImageFile().getFileName(),
                        command.getImageFile().getContentType(),
                        command.getImageFile().getFileSize(),
                        imageObjectName,
                        Map.of("bannerId", tempBannerId.toString())
                );
                uploadedImageUrl = "books/" + imageObjectName;
            } catch (Exception e) {
                log.error("Failed to upload banner image to MinIO for ID {}", tempBannerId, e);
                throw new BusinessValidationException("Lỗi khi tải ảnh banner lên hệ thống lưu trữ MinIO.");
            }
        }

        // Process icon upload via MinIO if file provided (outside DB transaction)
        if (command.getIconFile() != null) {
            try {
                iconObjectName = "banners/" + tempBannerId + "/icon_" + UUID.randomUUID() + "_" + command.getIconFile().getFileName();
                minioService.uploadFile(
                        command.getIconFile().getInputStream(),
                        command.getIconFile().getFileName(),
                        command.getIconFile().getContentType(),
                        command.getIconFile().getFileSize(),
                        iconObjectName,
                        Map.of("bannerId", tempBannerId.toString())
                );
                uploadedIconUrl = "books/" + iconObjectName;
            } catch (Exception e) {
                log.error("Failed to upload banner icon to MinIO for ID {}", tempBannerId, e);
            }
        }

        // Validate image URL
        if (uploadedImageUrl == null || uploadedImageUrl.trim().isEmpty()) {
            throw new BusinessValidationException(BannerMessageConstants.BANNER_IMAGE_REQUIRED);
        }

        final String finalImageUrl = uploadedImageUrl;
        final String finalIconUrl = uploadedIconUrl;
        final String finalImageObjectName = imageObjectName;
        final String finalIconObjectName = iconObjectName;

        try {
            return transactionTemplate.execute(status -> {
                Banner banner = Banner.builder()
                        .title(req.getTitle())
                        .subtitle(req.getSubtitle())
                        .imageUrl(finalImageUrl)
                        .iconUrl(finalIconUrl)
                        .linkUrl(req.getLinkUrl())
                        .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0)
                        .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                        .startDate(req.getStartDate())
                        .endDate(req.getEndDate())
                        .build();

                banner = bannerRepository.save(banner);
                UUID bannerId = banner.getId();

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
            });
        } catch (Exception ex) {
            if (finalImageObjectName != null) {
                try { minioService.deleteFile(finalImageObjectName); } catch (Exception ignored) {}
            }
            if (finalIconObjectName != null) {
                try { minioService.deleteFile(finalIconObjectName); } catch (Exception ignored) {}
            }
            throw ex;
        }
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
