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
import org.springframework.transaction.annotation.Transactional;

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

        @Value("${" + KeyConstants.STORAGE_PUBLIC_URL + "}")
        private String publicUrl;

        @Value("${" + KeyConstants.MINIO_USE_SSL + ":false}")
        private boolean useSsl;

        @Override
        @Transactional
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

                // Fetch Badge (DB check - cannot be annotated)
                Badge badge = null;
                if (cmd.getBadgeId() != null) {
                        badge = badgeRepository.findById(cmd.getBadgeId())
                                        .orElseThrow(() -> new BusinessValidationException(
                                                        BookMessageConstants.BADGE_NOT_FOUND,
                                                        BookMessageConstants.CODE_BADGE_NOT_FOUND));
                }

                // Fetch Categories (DB check - cannot be annotated)
                Set<Category> categories = new HashSet<>();
                if (cmd.getCategoryIds() != null && !cmd.getCategoryIds().isEmpty()) {
                        List<Category> categoryList = categoryRepository.findAllById(cmd.getCategoryIds());
                        if (categoryList.size() != cmd.getCategoryIds().size()) {
                                throw new BusinessValidationException(BookMessageConstants.CATEGORY_NOT_FOUND,
                                                BookMessageConstants.CODE_CATEGORY_NOT_FOUND);
                        }
                        categories.addAll(categoryList);
                }

                // Build and save the Book
                Book book = Book.builder()
                                .title(cmd.getTitle())
                                .introduce(cmd.getIntroduce())
                                .description(cmd.getDescription())
                                .thumbnailUrl("")
                                .active(false)
                                .badge(badge)
                                .categories(categories)
                                .build();

                book = bookRepository.save(book);
                UUID bookId = book.getId();

                // Resize cover image to 400x400 jpeg and build standard object path
                UploadFileModel resizedFile = ImageHelper.resizeTo400x400(
                                cmd.getCoverFileStream(),
                                cmd.getCoverFileName(),
                                cmd.getCoverContentType());

                String ext = ".jpg";
                String slugTitle = SlugHelper.toSlug(cmd.getTitle());
                String objectName = bookId.toString() + "_" + slugTitle + ext;
                String relativePath = "books/" + objectName;

                book.setThumbnailUrl(relativePath);
                book = bookRepository.save(book);

                // Fetch Authors and Map Book-Authors
                if (cmd.getAuthorIds() != null && !cmd.getAuthorIds().isEmpty()) {
                        List<Author> authors = authorRepository.findAllById(cmd.getAuthorIds());
                        if (authors.size() != cmd.getAuthorIds().size()) {
                                throw new BusinessValidationException(BookMessageConstants.AUTHOR_NOT_FOUND,
                                                BookMessageConstants.CODE_AUTHOR_NOT_FOUND);
                        }

                        for (Author author : authors) {
                                BookAuthor bookAuthor = BookAuthor.builder()
                                                .book(book)
                                                .author(author)
                                                .active(true)
                                                .build();
                                bookAuthorRepository.save(bookAuthor);
                        }
                }

                // Upload Cover File synchronously to MinIO
                try {
                        minioService.uploadFile(
                                        resizedFile.getInputStream(),
                                        resizedFile.getFileName(),
                                        resizedFile.getContentType(),
                                        resizedFile.getFileSize(),
                                        objectName,
                                        null);
                } catch (Exception ex) {
                        log.error("Failed to upload book cover to MinIO. Book ID: {}", bookId, ex);
                        throw new BusinessValidationException(BookMessageConstants.UPLOAD_FAILED + ex.getMessage(),
                                        BookMessageConstants.CODE_UPLOAD_FAILED);
                }

                // Construct response
                String scheme = useSsl ? "https" : "http";
                String cleanBaseUrl = publicUrl.replaceAll("^https?://", "").replaceAll("/+$", "");
                String absoluteThumbnailUrl = scheme + "://" + cleanBaseUrl + "/" + relativePath;

                List<String> authorNames = new ArrayList<>();
                if (cmd.getAuthorIds() != null && !cmd.getAuthorIds().isEmpty()) {
                        authorNames = authorRepository.findAllById(cmd.getAuthorIds()).stream()
                                        .map(Author::getName)
                                        .toList();
                }

                log.info("Book created successfully. ID: {}, Title: {}", bookId, book.getTitle());

                return BookResponse.builder()
                                .id(bookId)
                                .title(book.getTitle())
                                .introduce(book.getIntroduce())
                                .thumbnailUrl(absoluteThumbnailUrl)
                                .badgeText(badge != null ? badge.getText() : null)
                                .badgeTextColor(badge != null ? badge.getTextColor() : null)
                                .badgeBgColor(badge != null ? badge.getBgColor() : null)
                                .minPrice(BigDecimal.ZERO)
                                .priceDisplay("")
                                .wasPriceDisplay("")
                                .authors(authorNames)
                                .build();
        }
}