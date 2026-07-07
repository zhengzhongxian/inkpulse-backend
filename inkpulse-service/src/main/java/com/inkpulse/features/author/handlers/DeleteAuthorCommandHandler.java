package com.inkpulse.features.author.handlers;

import com.inkpulse.cache.CacheProperties;
import com.inkpulse.cache.ICacheService;
import com.inkpulse.constants.KeyConstants;
import com.inkpulse.constants.message.BookMessageConstants;
import com.inkpulse.constants.QueueConstants;
import com.inkpulse.service.outbox.OutboxPublisher;
import com.inkpulse.features.author.dto.SyncAuthorMessage;
import com.inkpulse.corehelpers.exceptions.ResourceNotFoundException;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.*;
import com.inkpulse.features.author.commands.DeleteAuthorCommand;
import com.inkpulse.features.book.dto.SyncBookEditionMessage;
import com.inkpulse.repositories.AuthorRepository;
import com.inkpulse.repositories.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteAuthorCommandHandler implements Command.CommandHandler<DeleteAuthorCommand, Boolean> {

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final OutboxPublisher outboxPublisher;
    private final ICacheService cacheService;
    private final CacheProperties cacheProperties;

    @Override
    @Transactional
    public Boolean handle(DeleteAuthorCommand cmd) {
        if (cmd.getId() == null) {
            throw new ResourceNotFoundException(BookMessageConstants.SINGLE_AUTHOR_NOT_FOUND);
        }

        Author author = authorRepository.findById(cmd.getId())
                .orElseThrow(() -> new ResourceNotFoundException(BookMessageConstants.SINGLE_AUTHOR_NOT_FOUND));

        author.setDeleted(true);
        authorRepository.save(author);

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

        SyncAuthorMessage syncMsg = SyncAuthorMessage.builder()
                .id(author.getId())
                .isDeleted(true)
                .build();
        outboxPublisher.publish(
                QueueConstants.SYNC_AUTHOR,
                syncMsg,
                "urn:message:InkPulse.Worker.Features.Book.Messages:SyncAuthorMessage");
        log.info("Author deletion sync message published to outbox. ID: {}", author.getId());

        // Propagate to all affected book editions
        propagateAuthorDeletionToBookEditions(author.getId());

        return true;
    }

    private void propagateAuthorDeletionToBookEditions(UUID authorId) {
        List<Book> books = bookRepository.findBooksByAuthorId(authorId);
        for (Book book : books) {
            String authorNameJoined = book.getBookAuthors().stream()
                    .filter(ba -> ba.isActive() && ba.getAuthor() != null)
                    .map(ba -> ba.getAuthor().getName())
                    .reduce((a, b) -> a + ", " + b).orElse("");

            for (BookEdition edition : book.getEditions()) {
                if (edition.isDeleted())
                    continue;

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
                        .widthCm(edition.getWidthCm())
                        .heightCm(edition.getHeightCm())
                        .lengthCm(edition.getLengthCm())
                        .weightGram(edition.getWeightGram())
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

                // Evict edition detail cache
                try {
                    cacheService.remove(cacheProperties.buildKey(KeyConstants.SECTION_BOOK_EDITION_DETAIL,
                            edition.getId().toString()));
                } catch (Exception ex) {
                    log.error("Failed to evict cache for edition ID: {}", edition.getId(), ex);
                }
            }
        }

        // Invalidate author editions Redis Set
        try {
            String setKey = cacheProperties.buildKey(KeyConstants.SECTION_AUTHOR_EDITIONS, authorId.toString());
            Set<String> editionIds = cacheService.smembers(setKey);
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

        log.info("Published outbox messages to propagate author deletion. Author ID: {}", authorId);
    }
}