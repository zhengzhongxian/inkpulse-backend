package com.inkpulse.features.book.handlers;

import com.inkpulse.constants.KeyConstants;
import com.inkpulse.constants.message.BookMessageConstants;
import com.inkpulse.constants.QueueConstants;
import com.inkpulse.corehelpers.SlugHelper;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.corehelpers.exceptions.ResourceNotFoundException;
import com.inkpulse.corehelpers.images.ImageHelper;
import com.inkpulse.corehelpers.images.UploadFileModel;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.*;
import com.inkpulse.features.book.commands.UpdateBookCommand;
import com.inkpulse.cache.CacheProperties;
import com.inkpulse.cache.ICacheService;
import com.inkpulse.models.response.book.BookResponse;
import com.inkpulse.features.book.dto.SyncBookEditionMessage;
import com.inkpulse.repositories.*;
import com.inkpulse.service.minio.IMinioService;
import com.inkpulse.service.outbox.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateBookCommandHandler implements Command.CommandHandler<UpdateBookCommand, BookResponse> {

        private final BookRepository bookRepository;
        private final CategoryRepository categoryRepository;
        private final AuthorRepository authorRepository;
        private final BookAuthorRepository bookAuthorRepository;
        private final BadgeRepository badgeRepository;
        private final IMinioService minioService;
        private final OutboxPublisher outboxPublisher;
        private final ICacheService cacheService;
        private final CacheProperties cacheProperties;
        private final TransactionTemplate transactionTemplate;

        @Value("${" + KeyConstants.STORAGE_PUBLIC_URL + "}")
        private String publicUrl;

        @Value("${" + KeyConstants.MINIO_USE_SSL + ":false}")
        private boolean useSsl;

        @Override
        public BookResponse handle(UpdateBookCommand cmd) {
                if (cmd.getId() == null) {
                        throw new ResourceNotFoundException(BookMessageConstants.BOOK_NOT_FOUND);
                }

                Book book = bookRepository.findById(cmd.getId())
                                .orElseThrow(() -> new ResourceNotFoundException(BookMessageConstants.BOOK_NOT_FOUND));

                if (cmd.getTitle() == null || cmd.getTitle().isBlank()) {
                        throw new BusinessValidationException(BookMessageConstants.TITLE_EMPTY,
                                        BookMessageConstants.CODE_TITLE_EMPTY);
                }

                // 1. Update Badge
                Badge badge = null;
                if (cmd.getBadgeId() != null) {
                        badge = badgeRepository.findById(cmd.getBadgeId())
                                        .orElseThrow(() -> new BusinessValidationException(
                                                        BookMessageConstants.BADGE_NOT_FOUND,
                                                        BookMessageConstants.CODE_BADGE_NOT_FOUND));
                }

                // 2. Update Categories
                Set<Category> categories = new HashSet<>();
                if (cmd.getCategoryIds() != null && !cmd.getCategoryIds().isEmpty()) {
                        List<Category> categoryList = categoryRepository.findAllById(cmd.getCategoryIds());
                        if (categoryList.size() != cmd.getCategoryIds().size()) {
                                throw new BusinessValidationException(BookMessageConstants.CATEGORY_NOT_FOUND,
                                                BookMessageConstants.CODE_CATEGORY_NOT_FOUND);
                        }
                        categories.addAll(categoryList);
                }

                String newRelativePath = null;
                String newObjectName = null;
                String oldObjectNameToDelete = null;

                // 3. Update Cover File if provided (outside DB transaction)
                if (cmd.getCoverFileStream() != null) {
                        // Validate image constraints (max 5MB)
                        ImageHelper.validateImage(
                                        cmd.getCoverContentType(),
                                        cmd.getCoverFileSize(),
                                        5 * 1024 * 1024L);

                        UploadFileModel resizedFile = ImageHelper.resizeTo400x400(
                                        cmd.getCoverFileStream(),
                                        cmd.getCoverFileName(),
                                        cmd.getCoverContentType());

                        String ext = ".jpg";
                        String slugTitle = SlugHelper.toSlug(cmd.getTitle());
                        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
                        newObjectName = book.getId().toString() + "_" + slugTitle + "_" + uniqueSuffix + ext;
                        newRelativePath = "books/" + newObjectName;

                        try {
                                minioService.uploadFile(
                                                resizedFile.getInputStream(),
                                                resizedFile.getFileName(),
                                                resizedFile.getContentType(),
                                                resizedFile.getFileSize(),
                                                newObjectName,
                                                null);
                                if (book.getThumbnailUrl() != null && !book.getThumbnailUrl().isBlank() && book.getThumbnailUrl().contains("/")) {
                                        oldObjectNameToDelete = book.getThumbnailUrl().substring(book.getThumbnailUrl().lastIndexOf("/") + 1);
                                }
                        } catch (Exception ex) {
                                log.error("Failed to upload updated book cover to MinIO. Book ID: {}", book.getId(), ex);
                                throw new BusinessValidationException(
                                                BookMessageConstants.UPLOAD_FAILED + ex.getMessage(),
                                                BookMessageConstants.CODE_UPLOAD_FAILED);
                        }
                }

                final Badge finalBadge = badge;
                final Set<Category> finalCategories = categories;
                final String finalNewRelativePath = newRelativePath;
                final String finalNewObjectName = newObjectName;
                final String finalOldObjectNameToDelete = oldObjectNameToDelete;

                try {
                        return transactionTemplate.execute(status -> {
                                book.setBadge(finalBadge);
                                book.setCategories(finalCategories);
                                if (finalNewRelativePath != null) {
                                        book.setThumbnailUrl(finalNewRelativePath);
                                }

                                // 4. Update basic info
                                book.setTitle(cmd.getTitle());
                                book.setIntroduce(cmd.getIntroduce());
                                book.setDescription(cmd.getDescription());

                                if (cmd.getActive() != null) {
                                        if (cmd.getActive()) {
                                                long validEditionCount = book.getEditions() != null
                                                                ? book.getEditions().stream().filter(e -> !e.isDeleted()).count()
                                                                : 0;
                                                if (validEditionCount == 0) {
                                                        throw new BusinessValidationException(
                                                                        BookMessageConstants.BOOK_HAS_NO_EDITION,
                                                                        BookMessageConstants.CODE_BOOK_HAS_NO_EDITION);
                                                }
                                        }
                                        book.setActive(cmd.getActive());
                                }

                                // 5. Update Authors association in-place
                                Set<UUID> requestedAuthorIds = cmd.getAuthorIds() != null
                                                ? new HashSet<>(cmd.getAuthorIds())
                                                : new HashSet<>();

                                Set<BookAuthor> existingBookAuthors = book.getBookAuthors();
                                if (existingBookAuthors == null) {
                                        existingBookAuthors = new HashSet<>();
                                        book.setBookAuthors(existingBookAuthors);
                                }

                                existingBookAuthors.removeIf(ba -> !requestedAuthorIds.contains(ba.getAuthor().getId()));

                                Set<UUID> currentAuthorIds = existingBookAuthors.stream()
                                                .map(ba -> ba.getAuthor().getId())
                                                .collect(Collectors.toSet());

                                Set<UUID> newAuthorIds = requestedAuthorIds.stream()
                                                .filter(id -> !currentAuthorIds.contains(id))
                                                .collect(Collectors.toSet());

                                List<String> authorNames = new ArrayList<>();
                                for (BookAuthor ba : existingBookAuthors) {
                                        authorNames.add(ba.getAuthor().getName());
                                }

                                if (!newAuthorIds.isEmpty()) {
                                        List<Author> newAuthors = authorRepository.findAllById(newAuthorIds);
                                        if (newAuthors.size() != newAuthorIds.size()) {
                                                throw new BusinessValidationException(BookMessageConstants.AUTHOR_NOT_FOUND,
                                                                BookMessageConstants.CODE_AUTHOR_NOT_FOUND);
                                        }

                                        for (Author author : newAuthors) {
                                                BookAuthor bookAuthor = BookAuthor.builder()
                                                                .book(book)
                                                                .author(author)
                                                                .active(true)
                                                                .build();
                                                existingBookAuthors.add(bookAuthor);
                                                authorNames.add(author.getName());
                                        }
                                }

                                Book savedBook = bookRepository.save(book);

                                // Delete old cover file if new file was uploaded successfully
                                if (finalOldObjectNameToDelete != null) {
                                        try {
                                                minioService.deleteFile(finalOldObjectNameToDelete);
                                        } catch (Exception e) {
                                                log.warn("Failed to delete old book cover file from MinIO: {}", finalOldObjectNameToDelete, e);
                                        }
                                }

                                // 6. Notify ELS about book metadata changes for all of its editions
                                String authorNameJoined = authorNames.stream().collect(Collectors.joining(", "));
                                List<String> categorySlugs = finalCategories.stream().map(Category::getSlug).toList();

                                for (BookEdition edition : savedBook.getEditions()) {
                                        List<SyncBookEditionMessage.BadgeInfo> editionBadges = new ArrayList<>();
                                        if (edition.getBadges() != null) {
                                                editionBadges = edition.getBadges().stream()
                                                                .filter(eb -> eb.getBadge() != null && !eb.isDeleted())
                                                                .map(eb -> SyncBookEditionMessage.BadgeInfo.builder()
                                                                                .text(eb.getBadge().getText())
                                                                                .textColor(eb.getBadge().getTextColor())
                                                                                .bgColor(eb.getBadge().getBgColor())
                                                                                .build())
                                                                .toList();
                                        }

                                        SyncBookEditionMessage edMsg = SyncBookEditionMessage.builder()
                                                        .id(edition.getId())
                                                        .bookId(savedBook.getId())
                                                        .title(savedBook.getTitle())
                                                        .introduce(savedBook.getIntroduce())
                                                        .description(savedBook.getDescription())
                                                        .bookThumbnailUrl(savedBook.getThumbnailUrl())
                                                        .isbn(edition.getIsbn())
                                                        .price(edition.getPrice())
                                                        .oldPrice(edition.getOldPrice())
                                                        .stockQuantity(edition.getStockQuantity())
                                                        .editionNumber(edition.getEditionNumber())
                                                        .thumbnailUrl(edition.getThumbnailUrl())
                                                        .filePathPdf(edition.getFilePathPdf())
                                                        .coverType(edition.getCoverType() != null ? edition.getCoverType().name()
                                                                        : null)
                                                        .pageCount(edition.getPageCount())
                                                        .publicationYear(edition.getPublicationYear())
                                                        .widthCm(edition.getWidthCm())
                                                        .heightCm(edition.getHeightCm())
                                                        .lengthCm(edition.getLengthCm())
                                                        .weightGram(edition.getWeightGram())
                                                        .language(edition.getLanguage())
                                                        .publisherName(edition.getPublisher() != null ? edition.getPublisher().getName()
                                                                        : null)
                                                        .authorName(authorNameJoined)
                                                        .badgeText(finalBadge != null ? finalBadge.getText() : null)
                                                        .badgeTextColor(finalBadge != null ? finalBadge.getTextColor() : null)
                                                        .badgeBgColor(finalBadge != null ? finalBadge.getBgColor() : null)
                                                        .active(savedBook.isActive())
                                                        .deleted(savedBook.isDeleted() || edition.isDeleted())
                                                        .categorySlugs(categorySlugs)
                                                        .imageUrls(edition.getImages().stream().map(EditionImage::getImageUrl).toList())
                                                        .badges(editionBadges)
                                                        .build();

                                        outboxPublisher.publish(
                                                        QueueConstants.SYNC_BOOK_EDITION,
                                                        edMsg,
                                                        "urn:message:InkPulse.Worker.Features.Book.Messages:SyncBookEditionMessage");

                                        // Evict edition detail cache
                                        try {
                                                cacheService.remove(cacheProperties.buildKey(KeyConstants.SECTION_BOOK_EDITION_DETAIL,
                                                                edition.getId().toString()));
                                        } catch (Exception ex) {
                                                log.error("Failed to evict cache for edition ID: {}", edition.getId(), ex);
                                        }
                                }

                                // Evict the Book ID fallback cache key explicitly
                                try {
                                        cacheService.remove(cacheProperties.buildKey(KeyConstants.SECTION_BOOK_EDITION_DETAIL,
                                                        savedBook.getId().toString()));
                                } catch (Exception ex) {
                                        log.error("Failed to evict fallback cache for book ID: {}", savedBook.getId(), ex);
                                }

                                String scheme = useSsl ? "https" : "http";
                                String cleanBaseUrl = publicUrl.replaceAll("^https?://", "").replaceAll("/+$", "");
                                String absoluteThumbnailUrl = scheme + "://" + cleanBaseUrl + "/" + savedBook.getThumbnailUrl();

                                BookEdition minEdition = savedBook.getEditions().stream()
                                                .filter(e -> e.getPrice() != null)
                                                .min(Comparator.comparing(BookEdition::getPrice))
                                                .orElse(null);
                                BigDecimal minPrice = minEdition != null ? minEdition.getPrice() : BigDecimal.ZERO;

                                log.info("Book updated successfully. ID: {}, Title: {}", savedBook.getId(), savedBook.getTitle());

                                return BookResponse.builder()
                                                .id(savedBook.getId())
                                                .title(savedBook.getTitle())
                                                .introduce(savedBook.getIntroduce())
                                                .thumbnailUrl(absoluteThumbnailUrl)
                                                .badgeText(finalBadge != null ? finalBadge.getText() : null)
                                                .badgeTextColor(finalBadge != null ? finalBadge.getTextColor() : null)
                                                .badgeBgColor(finalBadge != null ? finalBadge.getBgColor() : null)
                                                .minPrice(minPrice)
                                                .priceDisplay(
                                                                minPrice.compareTo(BigDecimal.ZERO) > 0
                                                                                ? "chỉ từ " + BookResponse.formatVnd(minPrice)
                                                                                : "")
                                                .wasPriceDisplay("")
                                                .authors(authorNames)
                                                .build();
                        });
                } catch (Exception ex) {
                        if (finalNewObjectName != null) {
                                try {
                                        minioService.deleteFile(finalNewObjectName);
                                } catch (Exception cleanupEx) {
                                        log.warn("Failed to cleanup new MinIO cover file: {}", finalNewObjectName, cleanupEx);
                                }
                        }
                        throw ex;
                }
        }
}
