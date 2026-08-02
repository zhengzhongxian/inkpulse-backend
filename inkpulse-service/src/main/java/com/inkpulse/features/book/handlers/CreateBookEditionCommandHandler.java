package com.inkpulse.features.book.handlers;

import com.inkpulse.constants.KeyConstants;
import com.inkpulse.constants.message.BookMessageConstants;
import com.inkpulse.corehelpers.SlugHelper;
import com.inkpulse.corehelpers.UrlHelper;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.corehelpers.images.ImageHelper;
import com.inkpulse.corehelpers.images.UploadFileModel;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.*;
import com.inkpulse.features.book.commands.CreateBookEditionCommand;
import com.inkpulse.models.response.book.BookEditionResponse;
import com.inkpulse.models.response.book.PublicBookEditionDetailResponse;
import com.inkpulse.constants.QueueConstants;
import com.inkpulse.service.outbox.OutboxPublisher;
import com.inkpulse.entities.enums.CoverType;
import com.inkpulse.features.book.dto.SyncBookEditionMessage;
import com.inkpulse.service.minio.IMinioService;
import com.inkpulse.repositories.*;
import com.inkpulse.cache.ICacheService;
import com.inkpulse.cache.CacheProperties;
import com.inkpulse.service.ai.IAIVisionGrpcService;
import com.inkpulse.service.ai.AIVisionRateLimiter;
import com.inkpulse.grpc.ai.ImageAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateBookEditionCommandHandler
        implements Command.CommandHandler<CreateBookEditionCommand, BookEditionResponse> {

    private final BookRepository bookRepository;
    private final BookEditionRepository bookEditionRepository;
    private final PublisherRepository publisherRepository;
    private final EditionImageRepository editionImageRepository;
    private final OutboxPublisher outboxPublisher;
    private final IMinioService minioService;
    private final BadgeRepository badgeRepository;
    private final EditionBadgeRepository editionBadgeRepository;
    private final ICacheService cacheService;
    private final CacheProperties cacheProperties;
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
    public BookEditionResponse handle(CreateBookEditionCommand cmd) {
        // 1. Validations
        if (cmd.getBookId() == null) {
            throw new BusinessValidationException(BookMessageConstants.BOOK_NOT_FOUND,
                    BookMessageConstants.CODE_BOOK_NOT_FOUND);
        }
        if (cmd.getIsbn() == null || cmd.getIsbn().isBlank()) {
            throw new BusinessValidationException(BookMessageConstants.ISBN_EMPTY,
                    BookMessageConstants.CODE_ISBN_EMPTY);
        }
        if (cmd.getPrice() == null || cmd.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessValidationException(BookMessageConstants.PRICE_INVALID,
                    BookMessageConstants.CODE_PRICE_INVALID);
        }
        // Fetch Parent Book
        Book book = bookRepository.findById(cmd.getBookId())
                .orElseThrow(() -> new BusinessValidationException(BookMessageConstants.BOOK_NOT_FOUND,
                        BookMessageConstants.CODE_BOOK_NOT_FOUND));

        // Fetch Publisher
        Publisher publisher = null;
        if (cmd.getPublisherId() != null) {
            publisher = publisherRepository.findById(cmd.getPublisherId())
                    .orElseThrow(() -> new BusinessValidationException(BookMessageConstants.PUBLISHER_NOT_FOUND,
                            BookMessageConstants.CODE_PUBLISHER_NOT_FOUND));
        }

        // 1. Save BookEdition to database first to get the auto-generated ID (prevents
        // merge issues)
        BookEdition edition = BookEdition.builder()
                .book(book)
                .widthCm(cmd.getWidthCm())
                .heightCm(cmd.getHeightCm())
                .lengthCm(cmd.getLengthCm())
                .weightGram(cmd.getWeightGram())
                .isbn(cmd.getIsbn())
                .price(cmd.getPrice())
                .oldPrice(cmd.getOldPrice())
                .stockQuantity(cmd.getStockQuantity())
                .editionNumber(cmd.getEditionNumber())
                .coverType(
                        cmd.getCoverType() != null ? CoverType.valueOf(cmd.getCoverType().trim().toUpperCase()) : null)
                .pageCount(cmd.getPageCount())
                .publicationYear(cmd.getPublicationYear())
                .language(cmd.getLanguage())
                .publisher(publisher)
                .build();

        edition = bookEditionRepository.save(edition);
        UUID editionId = edition.getId();
        String slugTitle = SlugHelper.toSlug(book.getTitle() + " " + cmd.getEditionNumber());

        // 2. Upload cover thumbnail to MinIO if provided
        String coverRelativePath = null;
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
                        coverFile.getContentType()
                );
                if (analysis != null && !analysis.getIsBook()) {
                    log.warn("AI Vision verification failed for file: {}. Reason: {}", coverFile.getFileName(), analysis.getReason());
                    throw new BusinessValidationException(BookMessageConstants.AI_VISION_NOT_A_BOOK,
                            BookMessageConstants.CODE_AI_VISION_NOT_A_BOOK);
                }
                log.info("AI Vision verification passed. Confidence: {}", analysis != null ? analysis.getConfidence() : 1.0);
            } catch (BusinessValidationException bve) {
                throw bve;
            } catch (Exception e) {
                // Fail-open strategy: log warning, proceed with upload when AI service is down
                log.warn("AI Vision verification service unavailable. Proceeding with upload (Fail-Open). Error: {}", e.getMessage());
            }

            // Validate image constraints (max 5MB)
            ImageHelper.validateImage(
                    coverFile.getContentType(),
                    coverFile.getFileSize(),
                    5 * 1024 * 1024L);

            UploadFileModel resizedFile = ImageHelper.resizeTo400x400(
                    new java.io.ByteArrayInputStream(coverBytes),
                    coverFile.getFileName(),
                    coverFile.getContentType());

            String ext = ".jpg";
            String coverObjectName = "editions/covers/" + editionId.toString() + "_" + slugTitle + ext;
            try {
                minioService.uploadFile(
                        resizedFile.getInputStream(),
                        resizedFile.getFileName(),
                        resizedFile.getContentType(),
                        resizedFile.getFileSize(),
                        coverObjectName,
                        null);
                coverRelativePath = "books/" + coverObjectName;
            } catch (Exception ex) {
                log.error("Failed to upload edition cover to MinIO. Edition ID: {}", editionId, ex);
                throw new BusinessValidationException(BookMessageConstants.UPLOAD_FAILED + ex.getMessage(),
                        BookMessageConstants.CODE_UPLOAD_FAILED);
            }
        }

        UploadFileModel pdfFile = cmd.getPdfFile();
        String pdfObjectName = null;
        if (pdfFile != null && pdfFile.getInputStream() != null) {
            String pdfExt = ".pdf";
            String originalPdfName = pdfFile.getFileName();
            if (originalPdfName != null) {
                int extIndex = originalPdfName.lastIndexOf('.');
                if (extIndex != -1) {
                    pdfExt = originalPdfName.substring(extIndex);
                }
            }
            String pdfPath = "editions/pdfs/" + editionId.toString() + "_" + slugTitle + pdfExt;
            try {
                minioService.uploadFile(
                        pdfFile.getInputStream(),
                        pdfFile.getFileName(),
                        pdfFile.getContentType(),
                        pdfFile.getFileSize(),
                        pdfPath,
                        null,
                        null);
                pdfObjectName = "pdf/" + pdfPath;
            } catch (Exception ex) {
                log.error("Failed to upload edition PDF to MinIO. Edition ID: {}", editionId, ex);
                throw new BusinessValidationException(
                        BookMessageConstants.PDF_UPLOAD_FAILED + ex.getMessage(),
                        BookMessageConstants.CODE_PDF_UPLOAD_FAILED);
            }
        }

        final String finalCoverObjectName = coverFile != null ? "editions/covers/" + editionId.toString() + "_" + slugTitle + ".jpg" : null;
        final String finalPdfPath = pdfFile != null ? "editions/pdfs/" + editionId.toString() + "_" + slugTitle + ".pdf" : null;
        final List<String> uploadedGalleryObjectNames = new ArrayList<>();
        if (cmd.getAdditionalImages() != null && !cmd.getAdditionalImages().isEmpty()) {
            for (int i = 0; i < cmd.getAdditionalImages().size(); i++) {
                uploadedGalleryObjectNames.add("editions/images/" + editionId.toString() + "/img_" + i + "_" + slugTitle);
            }
        }

        try {
            final String finalCoverRelativePath = coverRelativePath;
            final String finalPdfObjectName = pdfObjectName;

            return transactionTemplate.execute(status -> {
                edition.setThumbnailUrl(finalCoverRelativePath);
                edition.setFilePathPdf(finalPdfObjectName);
                BookEdition savedEdition = bookEditionRepository.save(edition);

                Set<EditionBadge> edBadges = new HashSet<>();
                if (cmd.getBadgeIds() != null && !cmd.getBadgeIds().isEmpty()) {
                    int order = 0;
                    Set<UUID> uniqueBadgeIds = new java.util.LinkedHashSet<>(cmd.getBadgeIds());
                    for (UUID badgeId : uniqueBadgeIds) {
                        Badge badge = badgeRepository.findById(badgeId).orElse(null);
                        if (badge != null) {
                            EditionBadge eb = EditionBadge.builder()
                                    .edition(savedEdition)
                                    .badge(badge)
                                    .displayOrder(order++)
                                    .build();
                            edBadges.add(eb);
                        }
                    }
                }
                savedEdition.setBadges(edBadges);

                List<String> imageRelativePaths = new ArrayList<>();
                List<UploadFileModel> additionalImages = cmd.getAdditionalImages();
                if (additionalImages != null && !additionalImages.isEmpty()) {
                    for (int i = 0; i < additionalImages.size(); i++) {
                        UploadFileModel imgFile = additionalImages.get(i);
                        if (imgFile == null || imgFile.getInputStream() == null)
                            continue;

                        ImageHelper.validateImage(
                                imgFile.getContentType(),
                                imgFile.getFileSize(),
                                5 * 1024 * 1024L);
                        UploadFileModel resizedImgFile = ImageHelper.resizeTo400x400(
                                imgFile.getInputStream(),
                                imgFile.getFileName(),
                                imgFile.getContentType());

                        String imgObjectName = "editions/images/" + editionId.toString() + "/img_" + i + "_" + slugTitle;
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
                                    .edition(savedEdition)
                                    .imageUrl("books/" + imgObjectName)
                                    .displayOrder(i)
                                    .build();
                            savedEdition.getImages().add(editionImage);
                        } catch (Exception ex) {
                            log.error("Failed to upload additional edition image at index {}. Edition ID: {}", i, editionId, ex);
                        }
                    }
                }

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
                        .id(editionId)
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
                        .thumbnailUrl(finalCoverRelativePath)
                        .filePathPdf(savedEdition.getFilePathPdf())
                        .coverType(savedEdition.getCoverType() != null ? savedEdition.getCoverType().name() : null)
                        .pageCount(savedEdition.getPageCount())
                        .publicationYear(savedEdition.getPublicationYear())
                        .widthCm(savedEdition.getWidthCm())
                        .heightCm(savedEdition.getHeightCm())
                        .lengthCm(savedEdition.getLengthCm())
                        .weightGram(savedEdition.getWeightGram())
                        .language(savedEdition.getLanguage())
                        .publisherName(publisher != null ? publisher.getName() : null)
                        .authorName(authorNameJoined)
                        .badgeText(badgeText)
                        .badgeTextColor(badgeTextColor)
                        .badgeBgColor(badgeBgColor)
                        .active(book.isActive())
                        .deleted(book.isDeleted())
                        .categorySlugs(categorySlugs)
                        .imageUrls(imageRelativePaths)
                        .badges(editionBadges)
                        .publisherId(publisher != null ? publisher.getId() : null)
                        .authorIds(authorIds)
                        .badgeIds(badgeIds)
                        .soldCount(savedEdition.getSoldCount())
                        .build();

                outboxPublisher.publish(
                        QueueConstants.SYNC_BOOK_EDITION,
                        syncMsg,
                        "urn:message:InkPulse.Worker.Features.Book.Messages:SyncBookEditionMessage");
                log.info("BookEdition sync message published to outbox. Edition ID: {}", editionId);

                try {
                    CacheProperties.SectionConfig detailSection = cacheProperties.getSections()
                            .get(KeyConstants.SECTION_BOOK_EDITION_DETAIL);
                    if (detailSection != null) {
                        cacheService.remove(detailSection.getKey() + editionId.toString());
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

                    if (detailSection != null) {
                        java.time.Duration cacheTtl = java.time.Duration.ofMinutes(detailSection.getTtl());
                        String cacheKey = detailSection.getKey() + editionId.toString();
                        
                        PublicBookEditionDetailResponse detailCacheObj = PublicBookEditionDetailResponse.builder()
                                .id(editionId)
                                .isbn(savedEdition.getIsbn())
                                .price(savedEdition.getPrice())
                                .oldPrice(savedEdition.getOldPrice())
                                .priceDisplay(BookEditionResponse.formatVnd(savedEdition.getPrice()))
                                .oldPriceDisplay(BookEditionResponse.formatVnd(savedEdition.getOldPrice()))
                                .stockQuantity(savedEdition.getStockQuantity())
                                .stockStatus(savedEdition.getStockQuantity() >= 10 ? "Còn hàng" : (savedEdition.getStockQuantity() > 0 ? "Chỉ còn " + savedEdition.getStockQuantity() + " cuốn" : "Tạm hết hàng"))
                                .editionNumber(savedEdition.getEditionNumber())
                                .soldCount(savedEdition.getSoldCount())
                                .thumbnailUrl(UrlHelper.buildAbsoluteUrl(publicUrl, finalCoverRelativePath, useSsl))
                                .coverType(savedEdition.getCoverType() != null ? savedEdition.getCoverType().name() : null)
                                .pageCount(savedEdition.getPageCount())
                                .publicationYear(savedEdition.getPublicationYear())
                                .weightGram(savedEdition.getWeightGram())
                                .widthCm(savedEdition.getWidthCm())
                                .heightCm(savedEdition.getHeightCm())
                                .lengthCm(savedEdition.getLengthCm())
                                .language(savedEdition.getLanguage())
                                .publisherName(publisher != null ? publisher.getName() : null)
                                .imageUrls(imageRelativePaths.stream().map(p -> UrlHelper.buildAbsoluteUrl(publicUrl, p, useSsl)).toList())
                                .bookId(book.getId())
                                .bookTitle(book.getTitle())
                                .bookThumbnailUrl(UrlHelper.buildAbsoluteUrl(publicUrl, book.getThumbnailUrl(), useSsl))
                                .introduce(book.getIntroduce())
                                .description(book.getDescription())
                                .authorName(authorNameJoined)
                                .badgeText(badgeText)
                                .badgeTextColor(badgeTextColor)
                                .badgeBgColor(badgeBgColor)
                                .categorySlugs(categorySlugs)
                                .isFlashSale(false)
                                .flashSaleItemId(null)
                                .build();

                        cacheService.set(cacheKey, detailCacheObj, cacheTtl);

                        String bookSetKey = cacheProperties.buildKey(KeyConstants.SECTION_BOOK_EDITIONS, book.getId().toString());
                        cacheService.sadd(bookSetKey, cacheTtl, editionId.toString());
                        log.info("Cached newly created BookEdition detail in Redis. Key: {}", cacheKey);
                    }
                } catch (Exception e) {
                    log.error("Failed to evict/populate Redis cache key for Book ID: {}", book.getId(), e);
                }

                String absoluteThumbnailUrl = UrlHelper.buildAbsoluteUrl(publicUrl, finalCoverRelativePath, useSsl);
                String absolutePdfUrl = UrlHelper.buildAbsoluteUrl(pdfPublicUrl, finalPdfObjectName, useSsl);
                List<String> absoluteImageUrls = imageRelativePaths.stream()
                        .map(path -> UrlHelper.buildAbsoluteUrl(publicUrl, path, useSsl))
                        .toList();

                return BookEditionResponse.builder()
                        .id(editionId)
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
                        .publisherName(publisher != null ? publisher.getName() : null)
                        .build();
            });
        } catch (Exception ex) {
            // Compensating Cleanup: delete any uploaded files from MinIO if DB transaction failed
            if (finalCoverObjectName != null) {
                try { minioService.deleteFile(finalCoverObjectName); } catch (Exception ignored) {}
            }
            if (finalPdfPath != null) {
                try { minioService.deleteFile(finalPdfPath); } catch (Exception ignored) {}
            }
            for (String imgName : uploadedGalleryObjectNames) {
                try { minioService.deleteFile(imgName); } catch (Exception ignored) {}
            }
            throw ex;
        }
    }
}
