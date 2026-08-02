package com.inkpulse.features.book.handlers;

import com.inkpulse.constants.KeyConstants;
import com.inkpulse.constants.message.BookMessageConstants;
import com.inkpulse.corehelpers.SlugHelper;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.corehelpers.images.ImageHelper;
import com.inkpulse.corehelpers.images.UploadFileModel;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.*;
import com.inkpulse.features.book.commands.CreateBookCommand;
import com.inkpulse.models.response.book.BookResponse;
import com.inkpulse.service.minio.IMinioService;
import com.inkpulse.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateBookCommandHandler implements Command.CommandHandler<CreateBookCommand, BookResponse> {

        private final BookRepository bookRepository;
        private final CategoryRepository categoryRepository;
        private final AuthorRepository authorRepository;
        private final BookAuthorRepository bookAuthorRepository;
        private final BadgeRepository badgeRepository;
        private final IMinioService minioService;
        private final TransactionTemplate transactionTemplate;

        @Value("${" + KeyConstants.STORAGE_PUBLIC_URL + "}")
        private String publicUrl;

        @Value("${" + KeyConstants.MINIO_USE_SSL + ":false}")
        private boolean useSsl;

        @Override
        public BookResponse handle(CreateBookCommand cmd) {
                // Validate image (byte stream - cannot be annotated)
                try {
                        ImageHelper.validateImage(
                                        cmd.getCoverContentType(),
                                        cmd.getCoverFileSize(),
                                        5 * 1024 * 1024L);
                } catch (Exception e) {
                        throw new BusinessValidationException(
                                        BookMessageConstants.Validate.IMAGE_INVALID + e.getMessage(),
                                        "IMAGE_VALIDATION_FAILED");
                }

                // Fetch Badge
                Badge badge = null;
                if (cmd.getBadgeId() != null) {
                        badge = badgeRepository.findById(cmd.getBadgeId())
                                        .orElseThrow(() -> new BusinessValidationException(
                                                        BookMessageConstants.BADGE_NOT_FOUND,
                                                        BookMessageConstants.CODE_BADGE_NOT_FOUND));
                }

                // Fetch Categories
                Set<Category> categories = new HashSet<>();
                if (cmd.getCategoryIds() != null && !cmd.getCategoryIds().isEmpty()) {
                        List<Category> categoryList = categoryRepository.findAllById(cmd.getCategoryIds());
                        if (categoryList.size() != cmd.getCategoryIds().size()) {
                                throw new BusinessValidationException(BookMessageConstants.CATEGORY_NOT_FOUND,
                                                BookMessageConstants.CODE_CATEGORY_NOT_FOUND);
                        }
                        categories.addAll(categoryList);
                }

                // Fetch Authors
                List<Author> authors = new ArrayList<>();
                if (cmd.getAuthorIds() != null && !cmd.getAuthorIds().isEmpty()) {
                        authors = authorRepository.findAllById(cmd.getAuthorIds());
                        if (authors.size() != cmd.getAuthorIds().size()) {
                                throw new BusinessValidationException(BookMessageConstants.AUTHOR_NOT_FOUND,
                                                BookMessageConstants.CODE_AUTHOR_NOT_FOUND);
                        }
                }

                // Resize cover image to 400x400 jpeg and build object path
                UploadFileModel resizedFile = ImageHelper.resizeTo400x400(
                                cmd.getCoverFileStream(),
                                cmd.getCoverFileName(),
                                cmd.getCoverContentType());

                String ext = ".jpg";
                String slugTitle = SlugHelper.toSlug(cmd.getTitle());
                UUID bookUuid = UUID.randomUUID();
                String objectName = bookUuid.toString() + "_" + slugTitle + ext;
                String relativePath = "books/" + objectName;

                // Upload Cover File synchronously to MinIO (outside DB transaction)
                try {
                        minioService.uploadFile(
                                        resizedFile.getInputStream(),
                                        resizedFile.getFileName(),
                                        resizedFile.getContentType(),
                                        resizedFile.getFileSize(),
                                        objectName,
                                        null);
                } catch (Exception ex) {
                        log.error("Failed to upload book cover to MinIO.", ex);
                        throw new BusinessValidationException(BookMessageConstants.UPLOAD_FAILED + ex.getMessage(),
                                        BookMessageConstants.CODE_UPLOAD_FAILED);
                }

                final Badge finalBadge = badge;
                final List<Author> finalAuthors = authors;

                try {
                        return transactionTemplate.execute(status -> {
                                Book book = Book.builder()
                                                .title(cmd.getTitle())
                                                .introduce(cmd.getIntroduce())
                                                .description(cmd.getDescription())
                                                .thumbnailUrl(relativePath)
                                                .active(false)
                                                .badge(finalBadge)
                                                .categories(categories)
                                                .build();

                                Book savedBook = bookRepository.save(book);
                                UUID savedBookId = savedBook.getId();

                                for (Author author : finalAuthors) {
                                        BookAuthor bookAuthor = BookAuthor.builder()
                                                        .book(savedBook)
                                                        .author(author)
                                                        .active(true)
                                                        .build();
                                        bookAuthorRepository.save(bookAuthor);
                                }

                                String scheme = useSsl ? "https" : "http";
                                String cleanBaseUrl = publicUrl.replaceAll("^https?://", "").replaceAll("/+$", "");
                                String absoluteThumbnailUrl = scheme + "://" + cleanBaseUrl + "/" + relativePath;

                                List<String> authorNames = finalAuthors.stream()
                                                .map(Author::getName)
                                                .toList();

                                log.info("Book created successfully. ID: {}, Title: {}", savedBookId, savedBook.getTitle());

                                return BookResponse.builder()
                                                .id(savedBookId)
                                                .title(savedBook.getTitle())
                                                .introduce(savedBook.getIntroduce())
                                                .thumbnailUrl(absoluteThumbnailUrl)
                                                .badgeText(finalBadge != null ? finalBadge.getText() : null)
                                                .badgeTextColor(finalBadge != null ? finalBadge.getTextColor() : null)
                                                .badgeBgColor(finalBadge != null ? finalBadge.getBgColor() : null)
                                                .minPrice(BigDecimal.ZERO)
                                                .priceDisplay("")
                                                .wasPriceDisplay("")
                                                .authors(authorNames)
                                                .build();
                        });
                } catch (Exception ex) {
                        try {
                                minioService.deleteFile(objectName);
                                log.info("Cleaned up uploaded MinIO book cover file: {}", objectName);
                        } catch (Exception cleanupEx) {
                                log.warn("Failed to cleanup MinIO file after DB exception: {}", objectName, cleanupEx);
                        }
                        throw ex;
                }
        }
}