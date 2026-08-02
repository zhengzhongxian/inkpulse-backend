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
import org.springframework.transaction.support.TransactionTemplate;

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
    private final TransactionTemplate transactionTemplate;

    @Override
    public BannerResponse handle(UpdateBannerCommand command) {
        UUID bannerId = command.getBannerId();
        UpdateBannerRequest req = command.getRequest();
        log.info("Handling UpdateBannerCommand for ID: {}", bannerId);

        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new ResourceNotFoundException(BannerMessageConstants.BANNER_NOT_FOUND));

        String newImageUrl = null;
        String newIconUrl = null;
        String imageObjectName = null;
        String iconObjectName = null;

        // Update image via MinIO if file uploaded (outside DB transaction)
        if (command.getImageFile() != null) {
            try {
                imageObjectName = "banners/" + bannerId + "/image_" + UUID.randomUUID() + "_" + command.getImageFile().getFileName();
                minioService.uploadFile(
                        command.getImageFile().getInputStream(),
                        command.getImageFile().getFileName(),
                        command.getImageFile().getContentType(),
                        command.getImageFile().getFileSize(),
                        imageObjectName,
                        Map.of("bannerId", bannerId.toString())
                );
                newImageUrl = "books/" + imageObjectName;
            } catch (Exception e) {
                log.error("Failed to update banner image in MinIO for ID {}", bannerId, e);
                throw new BusinessValidationException("Lỗi khi tải ảnh banner mới lên MinIO.");
            }
        } else if (req.getImageUrl() != null && !req.getImageUrl().trim().isEmpty()) {
            newImageUrl = req.getImageUrl();
        }

        // Update icon via MinIO if file uploaded (outside DB transaction)
        if (command.getIconFile() != null) {
            try {
                iconObjectName = "banners/" + bannerId + "/icon_" + UUID.randomUUID() + "_" + command.getIconFile().getFileName();
                minioService.uploadFile(
                        command.getIconFile().getInputStream(),
                        command.getIconFile().getFileName(),
                        command.getIconFile().getContentType(),
                        command.getIconFile().getFileSize(),
                        iconObjectName,
                        Map.of("bannerId", bannerId.toString())
                );
                newIconUrl = "books/" + iconObjectName;
            } catch (Exception e) {
                log.error("Failed to update banner icon in MinIO for ID {}", bannerId, e);
            }
        } else if (req.getIconUrl() != null) {
            newIconUrl = req.getIconUrl();
        }

        final String finalImageUrl = newImageUrl;
        final String finalIconUrl = newIconUrl;
        final String finalImageObjectName = imageObjectName;
        final String finalIconObjectName = iconObjectName;

        try {
            return transactionTemplate.execute(status -> {
                banner.setTitle(req.getTitle());
                banner.setSubtitle(req.getSubtitle());
                if (req.getLinkUrl() != null) banner.setLinkUrl(req.getLinkUrl());
                if (req.getDisplayOrder() != null) banner.setDisplayOrder(req.getDisplayOrder());
                if (req.getIsActive() != null) banner.setIsActive(req.getIsActive());
                if (req.getStartDate() != null) banner.setStartDate(req.getStartDate());
                if (req.getEndDate() != null) banner.setEndDate(req.getEndDate());

                if (finalImageUrl != null) {
                    banner.setImageUrl(finalImageUrl);
                }
                if (finalIconUrl != null) {
                    banner.setIconUrl(finalIconUrl);
                }

                if (req.getEditionIds() != null) {
                    bannerEditionRepository.deleteByBannerId(bannerId);

                    List<BannerEdition> bannerEditions = new ArrayList<>();
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
                    banner.setBannerEditions(bannerEditions);
                }

                Banner savedBanner = bannerRepository.save(banner);
                evictBannerCaches(bannerId);

                return BannerResponse.builder()
                        .bannerId(savedBanner.getId())
                        .title(savedBanner.getTitle())
                        .subtitle(savedBanner.getSubtitle())
                        .imageUrl(savedBanner.getImageUrl())
                        .iconUrl(savedBanner.getIconUrl())
                        .linkUrl(savedBanner.getLinkUrl())
                        .displayOrder(savedBanner.getDisplayOrder())
                        .isActive(savedBanner.getIsActive())
                        .startDate(savedBanner.getStartDate())
                        .endDate(savedBanner.getEndDate())
                        .createdAt(savedBanner.getCreatedAt())
                        .updatedAt(savedBanner.getUpdatedAt())
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
