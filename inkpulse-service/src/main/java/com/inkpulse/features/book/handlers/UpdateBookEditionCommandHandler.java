package com.inkpulse.features.book.handlers;

import com.inkpulse.cache.CacheProperties;
import com.inkpulse.cache.ICacheService;
import com.inkpulse.service.ai.IAIVisionGrpcService;
import com.inkpulse.service.ai.AIVisionRateLimiter;
import com.inkpulse.grpc.ai.ImageAnalysisResponse;
import com.inkpulse.constants.KeyConstants;
import com.inkpulse.constants.message.BookMessageConstants;
import com.inkpulse.corehelpers.SlugHelper;
import com.inkpulse.corehelpers.UrlHelper;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.corehelpers.images.ImageHelper;
import com.inkpulse.corehelpers.images.UploadFileModel;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.*;
import com.inkpulse.features.book.commands.UpdateBookEditionCommand;
import com.inkpulse.models.response.book.BookEditionResponse;
import com.inkpulse.constants.QueueConstants;
import com.inkpulse.service.outbox.OutboxPublisher;
import com.inkpulse.entities.enums.CoverType;
import com.inkpulse.features.book.dto.SyncBookEditionMessage;
import com.inkpulse.service.minio.IMinioService;
import com.inkpulse.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateBookEditionCommandHandler
        implements Command.CommandHandler<UpdateBookEditionCommand, BookEditionResponse> {

    private final BookEditionRepository bookEditionRepository;
    private final PublisherRepository publisherRepository;
    private final OutboxPublisher outboxPublisher;
    private final IMinioService minioService;
    private final ICacheService cacheService;
    private final CacheProperties cacheProperties;
    private final BadgeRepository badgeRepository;
    private final EditionBadgeRepository editionBadgeRepository;
    private final IAIVisionGrpcService aiVisionGrpcService;
    private final AIVisionRateLimiter aiVisionRateLimiter;
    private final TransactionTemplate transactionTemplate;

    @Value("${" + KeyConstants.STORAGE_PUBLIC_URL + "}")
    private String publicUrl;

    @Value("${" + KeyConstants.STORAGE_PDF_PUBLIC_URL + "}")
    private String pdfPublicUrl;

    @Value("${" + KeyConstants.MINIO_USE_SSL + ":false}")
    private boolean useSsl;

    @Override
    public BookEditionResponse handle(UpdateBookEditionCommand cmd) {
        log.info("Handling UpdateBookEditionCommand for ID: {}", cmd.getId());

        // 1. Retrieve existing BookEdition
        if (cmd.getId() == null) {
            throw new BusinessValidationException(BookMessageConstants.BOOK_EDITION_NOT_FOUND,
                    BookMessageConstants.CODE_BOOK_EDITION_NOT_FOUND);
        }
        BookEdition edition = bookEditionRepository.findById(cmd.getId())
                .orElseThrow(() -> new BusinessValidationException(BookMessageConstants.BOOK_EDITION_NOT_FOUND,
                        BookMessageConstants.CODE_BOOK_EDITION_NOT_FOUND));

        Book book = edition.getBook();

        // 2. Validate input fields if present
        if (cmd.getIsbn() != null && cmd.getIsbn().isBlank()) {
            throw new BusinessValidationException(BookMessageConstants.ISBN_EMPTY,
                    BookMessageConstants.CODE_ISBN_EMPTY);
        }
        if (cmd.getPrice() != null && cmd.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessValidationException(BookMessageConstants.PRICE_INVALID,
                    BookMessageConstants.CODE_PRICE_INVALID);
        }

        String slugTitle = SlugHelper.toSlug(book.getTitle() + " "
                + (cmd.getEditionNumber() != null ? cmd.getEditionNumber() : edition.getEditionNumber()));

        // 3. Update fields
        if (cmd.getIsbn() != null) {
            edition.setIsbn(cmd.getIsbn());
        }
        if (cmd.getPrice() != null) {
            edition.setPrice(cmd.getPrice());
        }
        if (cmd.getOldPrice() != null) {
            edition.setOldPrice(cmd.getOldPrice());
        }
        if (cmd.getEditionNumber() != null) {
            edition.setEditionNumber(cmd.getEditionNumber());
        }
        if (cmd.getCoverType() != null) {
            edition.setCoverType(CoverType.valueOf(cmd.getCoverType().trim().toUpperCase()));
        }
        if (cmd.getPageCount() != null) {
            edition.setPageCount(cmd.getPageCount());
        }
        if (cmd.getPublicationYear() != null) {
            edition.setPublicationYear(cmd.getPublicationYear());
        }
        edition.setWidthCm(cmd.getWidthCm());
        edition.setHeightCm(cmd.getHeightCm());
        edition.setLengthCm(cmd.getLengthCm());
        edition.setWeightGram(cmd.getWeightGram());
        if (cmd.getLanguage() != null) {
            edition.setLanguage(cmd.getLanguage());
        }

        if (cmd.getPublisherId() != null) {
            Publisher publisher = publisherRepository.findById(cmd.getPublisherId())
                    .orElseThrow(() -> new BusinessValidationException(BookMessageConstants.PUBLISHER_NOT_FOUND,
                            BookMessageConstants.CODE_PUBLISHER_NOT_FOUND));
            edition.setPublisher(publisher);
        }

        // 4. Upload Cover Thumbnail if provided
        UploadFileModel coverFile = cmd.getCoverFile();
        if (coverFile != null && coverFile.getInputStream() != null) {
            byte[] coverBytes;
            try {
                coverBytes = coverFile.getInputStream().readAllBytes();
            } catch (Exception e) {
                log.error("Failed to read cover image bytes", e);
                throw new BusinessValidationException(BookMessageConstants.READ_COVER_ERROR,
                        BookMessageConstants.CODE_READ_COVER_ERROR);
            }

            // 1. Quota Check (Rate limit)
            if (!aiVisionRateLimiter.isAllowed(cmd.getAdminId())) {
                throw new BusinessValidationException(BookMessageConstants.AI_VISION_RATE_LIMIT_EXCEEDED,
                        BookMessageConstants.CODE_AI_VISION_RATE_LIMIT_EXCEEDED);
            }

            // 2. gRPC AI Vision Analysis
            try {
                ImageAnalysisResponse analysis = aiVisionGrpcService.analyzeImage(
                        coverBytes,
                        coverFile.getFileName(),
                        coverFile.getContentType());
                if (analysis != null && !analysis.getIsBook()) {
                    log.warn("AI Vision verification failed for file: {}. Reason: {}", coverFile.getFileName(),
                            analysis.getReason());
                    throw new BusinessValidationException(BookMessageConstants.AI_VISION_NOT_A_BOOK,
                            BookMessageConstants.CODE_AI_VISION_NOT_A_BOOK);
                }
                log.info("AI Vision verification passed. Confidence: {}",
                        analysis != null ? analysis.getConfidence() : 1.0);
            } catch (BusinessValidationException bve) {
                throw bve;
            } catch (Exception e) {
                // Fail-open strategy: log warning, proceed with upload when AI service is down
                log.warn("AI Vision verification service unavailable. Proceeding with upload (Fail-Open). Error: {}",
                        e.getMessage());
            }

            // Validate image constraints
            ImageHelper.validateImage(
                    coverFile.getContentType(),
                    coverFile.getFileSize(),
                    5 * 1024 * 1024L);

            UploadFileModel resizedFile = ImageHelper.resizeTo400x400(
                    new java.io.ByteArrayInputStream(coverBytes),
                    coverFile.getFileName(),
                    coverFile.getContentType());

            String ext = ".jpg";
            String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
            String coverObjectName = "editions/covers/" + edition.getId().toString() + "_" + slugTitle + "_" + uniqueSuffix + ext;
            try {
                minioService.uploadFile(
                        resizedFile.getInputStream(),
                        resizedFile.getFileName(),
                        resizedFile.getContentType(),
                        resizedFile.getFileSize(),
                        coverObjectName,
                        null);
                // Delete old cover image to prevent ghost cache and keep MinIO clean
                if (edition.getThumbnailUrl() != null && !edition.getThumbnailUrl().isBlank() && edition.getThumbnailUrl().contains("/")) {
                    String oldObjectName = edition.getThumbnailUrl().substring(edition.getThumbnailUrl().indexOf("editions/covers/"));
                    try {
                        minioService.deleteFile(oldObjectName);
                    } catch (Exception e) {
                        log.warn("Failed to delete old edition cover file from MinIO: {}", oldObjectName, e);
                    }
                }
                edition.setThumbnailUrl("books/" + coverObjectName);
            } catch (Exception ex) {
                log.error("Failed to upload updated edition cover to MinIO. Edition ID: {}", edition.getId(), ex);
                throw new BusinessValidationException(BookMessageConstants.UPLOAD_FAILED + ex.getMessage(),
                        BookMessageConstants.CODE_UPLOAD_FAILED);
            }
        }

        // 5. Upload PDF if provided
        UploadFileModel pdfFile = cmd.getPdfFile();
        if (pdfFile != null && pdfFile.getInputStream() != null) {
            String pdfExt = ".pdf";
            String originalPdfName = pdfFile.getFileName();
            if (originalPdfName != null) {
                int extIndex = originalPdfName.lastIndexOf('.');
                if (extIndex != -1) {
                    pdfExt = originalPdfName.substring(extIndex);
                }
            }
            String pdfObjectName = "editions/pdfs/" + edition.getId().toString() + "_" + slugTitle + pdfExt;
            try {
                minioService.uploadFile(
                        pdfFile.getInputStream(),
                        pdfFile.getFileName(),
                        pdfFile.getContentType(),
                        pdfFile.getFileSize(),
                        pdfObjectName,
                        null,
                        null);
                edition.setFilePathPdf("pdf/" + pdfObjectName);
            } catch (Exception ex) {
                log.error("Failed to upload updated edition PDF to MinIO. Edition ID: {}", edition.getId(), ex);
                throw new BusinessValidationException(
                        BookMessageConstants.PDF_UPLOAD_FAILED + ex.getMessage(),
                        BookMessageConstants.CODE_PDF_UPLOAD_FAILED);
            }
        }

        // 6. Upload and Save Additional Gallery Images if provided
        List<String> imageRelativePaths = new ArrayList<>();
        boolean isRetainProvided = cmd.getRetainImageUrls() != null;

        if (isRetainProvided || cmd.getAdditionalImages() != null) {
            List<String> retains = cmd.getRetainImageUrls() != null ? cmd.getRetainImageUrls() : new ArrayList<>();
            List<String> cleanRetains = retains.stream()
                    .map(url -> {
                        if (url.contains("editions/images/")) {
                            return "books/" + url.substring(url.indexOf("editions/images/"));
                        }
                        return url;
                    })
                    .toList();

            List<EditionImage> toRemove = new ArrayList<>();
            for (EditionImage img : edition.getImages()) {
                if (!cleanRetains.contains(img.getImageUrl())) {
                    toRemove.add(img);
                } else {
                    imageRelativePaths.add(img.getImageUrl());
                }
            }
            edition.getImages().removeAll(toRemove);

            List<UploadFileModel> additionalImages = cmd.getAdditionalImages();
            if (additionalImages != null && !additionalImages.isEmpty()) {
                int startIndex = edition.getImages().stream()
                        .mapToInt(EditionImage::getDisplayOrder)
                        .max()
                        .orElse(-1) + 1;

                for (int i = 0; i < additionalImages.size(); i++) {
                    UploadFileModel imgFile = additionalImages.get(i);
                    if (imgFile == null || imgFile.getInputStream() == null)
                        continue;

                    // Validate and resize gallery image to 400x400
                    ImageHelper.validateImage(
                            imgFile.getContentType(),
                            imgFile.getFileSize(),
                            5 * 1024 * 1024L);
                    UploadFileModel resizedImgFile = ImageHelper.resizeTo400x400(
                            imgFile.getInputStream(),
                            imgFile.getFileName(),
                            imgFile.getContentType());

                    String imgObjectName = "editions/images/" + edition.getId().toString() + "/img_"
                            + UUID.randomUUID().toString() + "_" + slugTitle;
                    try {
                        minioService.uploadFile(
                                resizedImgFile.getInputStream(),
                                resizedImgFile.getFileName(),
                                resizedImgFile.getContentType(),
                                resizedImgFile.getFileSize(),
                                imgObjectName,
                                null);
                        imageRelativePaths.add("books/" + imgObjectName);

                        EditionImage editionImage = EditionImage.builder()
                                .edition(edition)
                                .imageUrl("books/" + imgObjectName)
                                .displayOrder(startIndex + i)
                                .build();
                        edition.getImages().add(editionImage);
                    } catch (Exception ex) {
                        log.error("Failed to upload additional edition image at index {}. Edition ID: {}", i,
                                edition.getId(), ex);
                    }
                }
            }
        } else {
            // Keep existing images paths for ELS sync
            imageRelativePaths = edition.getImages().stream()
                    .map(EditionImage::getImageUrl)
                    .collect(Collectors.toList());
        }

        final List<String> finalImageRelativePaths = imageRelativePaths;

        return transactionTemplate.execute(status -> {
            // Update Badges
            if (cmd.getBadgeIds() != null) {
                editionBadgeRepository.deleteByEditionIdPhysical(edition.getId());
                if (edition.getBadges() != null) {
                    edition.getBadges().clear();
                }

                int order = 0;
                Set<UUID> uniqueBadgeIds = new java.util.LinkedHashSet<>(cmd.getBadgeIds());
                for (UUID badgeId : uniqueBadgeIds) {
                    Badge badge = badgeRepository.findById(badgeId).orElse(null);
                    if (badge != null) {
                        EditionBadge eb = EditionBadge.builder()
                                .edition(edition)
                                .badge(badge)
                                .displayOrder(order++)
                                .build();
                        edition.getBadges().add(eb);
                    }
                }
            }

            BookEdition savedEdition = bookEditionRepository.save(edition);

            String authorNameJoined = "";
            if (book.getBookAuthors() != null) {
                authorNameJoined = book.getBookAuthors().stream()
                        .filter(ba -> ba.isActive() && ba.getAuthor() != null)
                        .map(ba -> ba.getAuthor().getName())
                        .collect(Collectors.joining(", "));
            }

            List<String> categorySlugs = new ArrayList<>();
            if (book.getCategories() != null) {
                categorySlugs = book.getCategories().stream()
                        .map(Category::getSlug)
                        .toList();
            }

            String badgeText = book.getBadge() != null ? book.getBadge().getText() : null;
            String badgeTextColor = book.getBadge() != null ? book.getBadge().getTextColor() : null;
            String badgeBgColor = book.getBadge() != null ? book.getBadge().getBgColor() : null;

            List<SyncBookEditionMessage.BadgeInfo> editionBadges = new ArrayList<>();
            if (savedEdition.getBadges() != null) {
                editionBadges = savedEdition.getBadges().stream()
                        .filter(eb -> eb.getBadge() != null && !eb.isDeleted())
                        .map(eb -> SyncBookEditionMessage.BadgeInfo.builder()
                                .text(eb.getBadge().getText())
                                .textColor(eb.getBadge().getTextColor())
                                .bgColor(eb.getBadge().getBgColor())
                                .shape(eb.getBadge().getShape())
                                .build())
                        .toList();
            }

            List<UUID> authorIds = new ArrayList<>();
            if (book.getBookAuthors() != null) {
                authorIds = book.getBookAuthors().stream()
                        .filter(ba -> ba.isActive() && ba.getAuthor() != null)
                        .map(ba -> ba.getAuthor().getId())
                        .toList();
            }

            List<UUID> badgeIds = new ArrayList<>();
            if (savedEdition.getBadges() != null) {
                badgeIds = savedEdition.getBadges().stream()
                        .filter(eb -> eb.getBadge() != null && !eb.isDeleted())
                        .map(eb -> eb.getBadge().getId())
                        .toList();
            }

            SyncBookEditionMessage syncMsg = SyncBookEditionMessage.builder()
                    .id(savedEdition.getId())
                    .bookId(book.getId())
                    .title(book.getTitle())
                    .introduce(book.getIntroduce())
                    .description(book.getDescription())
                    .bookThumbnailUrl(book.getThumbnailUrl())
                    .isbn(savedEdition.getIsbn())
                    .price(savedEdition.getPrice())
                    .oldPrice(savedEdition.getOldPrice())
                    .stockQuantity(savedEdition.getStockQuantity())
                    .editionNumber(savedEdition.getEditionNumber())
                    .thumbnailUrl(savedEdition.getThumbnailUrl())
                    .filePathPdf(savedEdition.getFilePathPdf())
                    .coverType(savedEdition.getCoverType() != null ? savedEdition.getCoverType().name() : null)
                    .pageCount(savedEdition.getPageCount())
                    .publicationYear(savedEdition.getPublicationYear())
                    .widthCm(savedEdition.getWidthCm())
                    .heightCm(savedEdition.getHeightCm())
                    .lengthCm(savedEdition.getLengthCm())
                    .weightGram(savedEdition.getWeightGram())
                    .language(savedEdition.getLanguage())
                    .publisherName(savedEdition.getPublisher() != null ? savedEdition.getPublisher().getName() : null)
                    .authorName(authorNameJoined)
                    .badgeText(badgeText)
                    .badgeTextColor(badgeTextColor)
                    .badgeBgColor(badgeBgColor)
                    .active(book.isActive())
                    .deleted(book.isDeleted())
                    .categorySlugs(categorySlugs)
                    .imageUrls(finalImageRelativePaths)
                    .badges(editionBadges)
                    .publisherId(savedEdition.getPublisher() != null ? savedEdition.getPublisher().getId() : null)
                    .authorIds(authorIds)
                    .badgeIds(badgeIds)
                    .soldCount(savedEdition.getSoldCount())
                    .build();

            outboxPublisher.publish(
                    QueueConstants.SYNC_BOOK_EDITION,
                    syncMsg,
                    "urn:message:InkPulse.Worker.Features.Book.Messages:SyncBookEditionMessage");
            log.info("BookEdition updated sync message published to outbox. Edition ID: {}", savedEdition.getId());

            try {
                CacheProperties.SectionConfig detailSection = cacheProperties.getSections()
                        .get(KeyConstants.SECTION_BOOK_EDITION_DETAIL);
                if (detailSection != null) {
                    cacheService.remove(detailSection.getKey() + savedEdition.getId().toString());
                    cacheService.remove(detailSection.getKey() + book.getId().toString());

                    String bookSetKey = cacheProperties.buildKey(KeyConstants.SECTION_BOOK_EDITIONS,
                            book.getId().toString());
                    Set<String> editionIds = cacheService.smembers(bookSetKey);
                    if (editionIds != null && !editionIds.isEmpty()) {
                        for (String edId : editionIds) {
                            try {
                                cacheService.remove(detailSection.getKey() + edId);
                            } catch (Exception ex) {
                                log.error("Failed to evict edition detail cache for ID: {}", edId, ex);
                            }
                        }
                    }
                    cacheService.remove(bookSetKey);
                }
                log.info("Evicted Redis cache key for sister editions of Book ID: {} using Redis Set", book.getId());
            } catch (Exception e) {
                log.error("Failed to evict Redis cache key for sister editions of Book ID: {}", book.getId(), e);
            }

            String absoluteThumbnailUrl = UrlHelper.buildAbsoluteUrl(publicUrl, savedEdition.getThumbnailUrl(), useSsl);
            String absolutePdfUrl = UrlHelper.buildAbsoluteUrl(pdfPublicUrl, savedEdition.getFilePathPdf(), useSsl);
            List<String> absoluteImageUrls = finalImageRelativePaths.stream()
                    .map(path -> UrlHelper.buildAbsoluteUrl(publicUrl, path, useSsl))
                    .toList();

            return BookEditionResponse.builder()
                    .id(savedEdition.getId())
                    .bookId(book.getId())
                    .bookTitle(book.getTitle())
                    .isbn(savedEdition.getIsbn())
                    .price(savedEdition.getPrice())
                    .oldPrice(savedEdition.getOldPrice())
                    .priceDisplay(BookEditionResponse.formatVnd(savedEdition.getPrice()))
                    .oldPriceDisplay(BookEditionResponse.formatVnd(savedEdition.getOldPrice()))
                    .stockQuantity(savedEdition.getStockQuantity())
                    .editionNumber(savedEdition.getEditionNumber())
                    .thumbnailUrl(absoluteThumbnailUrl)
                    .imageUrls(absoluteImageUrls)
                    .filePathPdf(savedEdition.getFilePathPdf())
                    .filePathPdfUrl(absolutePdfUrl)
                    .soldCount(savedEdition.getSoldCount())
                    .ratingsCount(savedEdition.getRatingsCount())
                    .rating(savedEdition.getRating())
                    .coverType(savedEdition.getCoverType() != null ? savedEdition.getCoverType().name() : null)
                    .pageCount(savedEdition.getPageCount())
                    .publicationYear(savedEdition.getPublicationYear())
                    .widthCm(savedEdition.getWidthCm())
                    .heightCm(savedEdition.getHeightCm())
                    .lengthCm(savedEdition.getLengthCm())
                    .weightGram(savedEdition.getWeightGram())
                    .language(savedEdition.getLanguage())
                    .publisherName(savedEdition.getPublisher() != null ? savedEdition.getPublisher().getName() : null)
                    .build();
        });
    }
}
