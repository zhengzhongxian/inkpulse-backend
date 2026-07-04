package com.inkpulse.features.author.handlers;

import com.inkpulse.constants.KeyConstants;
import com.inkpulse.constants.message.BookMessageConstants;
import com.inkpulse.corehelpers.UrlHelper;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.corehelpers.exceptions.ResourceNotFoundException;
import com.inkpulse.corehelpers.images.ImageHelper;
import com.inkpulse.corehelpers.images.UploadFileModel;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.Author;
import com.inkpulse.entities.Book;
import com.inkpulse.entities.BookEdition;
import com.inkpulse.entities.Category;
import com.inkpulse.entities.EditionImage;
import com.inkpulse.features.author.commands.UpdateAuthorCommand;
import com.inkpulse.features.author.dto.AuthorResponse;
import com.inkpulse.cache.CacheProperties;
import com.inkpulse.cache.ICacheService;
import com.inkpulse.constants.QueueConstants;
import com.inkpulse.service.outbox.OutboxPublisher;
import com.inkpulse.features.author.dto.SyncAuthorMessage;
import com.inkpulse.features.book.dto.SyncBookEditionMessage;
import com.inkpulse.repositories.AuthorRepository;
import com.inkpulse.repositories.BookRepository;
import com.inkpulse.service.minio.IMinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateAuthorCommandHandler implements Command.CommandHandler<UpdateAuthorCommand, AuthorResponse> {

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final OutboxPublisher outboxPublisher;
    private final IMinioService minioService;
    private final ICacheService cacheService;
    private final CacheProperties cacheProperties;

    @Value("${" + KeyConstants.STORAGE_PUBLIC_URL + "}")
    private String publicUrl;

    @Value("${" + KeyConstants.MINIO_USE_SSL + ":false}")
    private boolean useSsl;

    @Override
    @Transactional
    public AuthorResponse handle(UpdateAuthorCommand cmd) {
        if (cmd.getId() == null) {
            throw new ResourceNotFoundException(BookMessageConstants.SINGLE_AUTHOR_NOT_FOUND);
        }
        if (cmd.getName() == null || cmd.getName().isBlank()) {
            throw new BusinessValidationException(BookMessageConstants.AUTHOR_NAME_EMPTY,
                    BookMessageConstants.CODE_AUTHOR_NAME_EMPTY);
        }

        Author author = authorRepository.findById(cmd.getId())
                .orElseThrow(() -> new ResourceNotFoundException(BookMessageConstants.SINGLE_AUTHOR_NOT_FOUND));

        String oldName = author.getName();
        author.setName(cmd.getName().trim());
        author.setBiography(cmd.getBiography() != null ? cmd.getBiography().trim() : null);

        UploadFileModel avatarFile = cmd.getAvatarFile();
        if (avatarFile != null && avatarFile.getInputStream() != null) {
            String ext = ".jpg";
            if (avatarFile.getFileName() != null) {
                int extIndex = avatarFile.getFileName().lastIndexOf('.');
                if (extIndex != -1) {
                    ext = avatarFile.getFileName().substring(extIndex);
                }
            }
            // Resize avatar to 400x400
            UploadFileModel resizedAvatar = ImageHelper.resizeTo400x400(
                    avatarFile.getInputStream(),
                    avatarFile.getFileName(),
                    avatarFile.getContentType());

            String avatarObjectName = "author/" + author.getId().toString() + "_avatar" + ext;
            try {
                minioService.uploadFile(
                        resizedAvatar.getInputStream(),
                        resizedAvatar.getFileName(),
                        resizedAvatar.getContentType(),
                        resizedAvatar.getFileSize(),
                        avatarObjectName,
                        null);
                author.setAvatar(avatarObjectName);
            } catch (Exception ex) {
                log.error("Failed to upload updated author avatar to MinIO. Author ID: {}", author.getId(), ex);
                throw new BusinessValidationException(BookMessageConstants.UPLOAD_FAILED + ex.getMessage(),
                        BookMessageConstants.CODE_UPLOAD_FAILED);
            }
        }

        author = authorRepository.save(author);

        // Evict Cache
        try {
            CacheProperties.SectionConfig section = cacheProperties.getSections()
                    .get(KeyConstants.SECTION_AUTHOR_DETAIL);
            if (section != null) {
                String cacheKey = section.getKey() + author.getId().toString();
                cacheService.remove(cacheKey);
                log.info("Evicted author detail cache for ID: {}", author.getId());
            }
        } catch (Exception ex) {
            log.error("Failed to evict author detail cache for ID: {}", author.getId(), ex);
        }

        SyncAuthorMessage authorMsg = SyncAuthorMessage.builder()
                .id(author.getId())
                .name(author.getName())
                .biography(author.getBiography())
                .avatarUrl(author.getAvatar())
                .isDeleted(false)
                .build();
        outboxPublisher.publish(
                QueueConstants.SYNC_AUTHOR,
                authorMsg,
                "urn:message:InkPulse.Worker.Features.Book.Messages:SyncAuthorMessage");
        log.info("Author updated sync message published to outbox. ID: {}", author.getId());

        if (!oldName.equals(author.getName())) {
            List<Book> books = bookRepository.findBooksByAuthorId(author.getId());
            for (Book book : books) {
                String authorNameJoined = book.getBookAuthors().stream()
                        .filter(ba -> ba.isActive() && ba.getAuthor() != null)
                        .map(ba -> ba.getAuthor().getName())
                        .collect(Collectors.joining(", "));

                for (BookEdition edition : book.getEditions()) {
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
                            .bookId(book.getId())
                            .title(book.getTitle())
                            .introduce(book.getIntroduce())
                            .description(book.getDescription())
                            .bookThumbnailUrl(book.getThumbnailUrl())
                            .isbn(edition.getIsbn())
                            .price(edition.getPrice())
                            .oldPrice(edition.getOldPrice())
                            .stockQuantity(edition.getStockQuantity())
                            .editionNumber(edition.getEditionNumber())
                            .thumbnailUrl(edition.getThumbnailUrl())
                            .filePathPdf(edition.getFilePathPdf())
                            .coverType(edition.getCoverType() != null ? edition.getCoverType().name() : null)
                            .pageCount(edition.getPageCount())
                            .publicationYear(edition.getPublicationYear())
                            .dimensions(edition.getDimensions())
                            .language(edition.getLanguage())
                            .publisherName(edition.getPublisher() != null ? edition.getPublisher().getName() : null)
                            .authorName(authorNameJoined)
                            .badgeText(book.getBadge() != null ? book.getBadge().getText() : null)
                            .badgeTextColor(book.getBadge() != null ? book.getBadge().getTextColor() : null)
                            .badgeBgColor(book.getBadge() != null ? book.getBadge().getBgColor() : null)
                            .active(book.isActive())
                            .deleted(book.isDeleted())
                            .categorySlugs(book.getCategories().stream().map(Category::getSlug).toList())
                            .imageUrls(edition.getImages().stream().map(EditionImage::getImageUrl).toList())
                            .badges(editionBadges)
                            .build();

                    outboxPublisher.publish(
                            QueueConstants.SYNC_BOOK_EDITION,
                            edMsg,
                            "urn:message:InkPulse.Worker.Features.Book.Messages:SyncBookEditionMessage");
                }
            }
            log.info(
                    "Published outbox messages to propagate author name change to denormalized book editions. Author ID: {}",
                    author.getId());
        }

        // Invalidate author editions Redis Set
        invalidateAuthorEditionsSet(author.getId());

        return AuthorResponse.builder()
                .id(author.getId())
                .name(author.getName())
                .biography(author.getBiography())
                .avatarUrl(UrlHelper.buildAbsoluteUrl(publicUrl, author.getAvatar(), useSsl))
                .build();
    }

    private void invalidateAuthorEditionsSet(UUID authorId) {
        try {
            String setKey = cacheProperties.buildKey(KeyConstants.SECTION_AUTHOR_EDITIONS, authorId.toString());
            java.util.Set<String> editionIds = cacheService.smembers(setKey);
            for (String edId : editionIds) {
                try {
                    cacheService.remove(cacheProperties.buildKey(KeyConstants.SECTION_BOOK_EDITION_DETAIL, edId));
                } catch (Exception ex) {
                    log.error("Failed to evict edition detail cache for edition ID: {}", edId, ex);
                }
            }
            cacheService.remove(setKey);
        } catch (Exception ex) {
            log.error("Failed to invalidate author editions set for author ID: {}", authorId, ex);
        }
    }
}
