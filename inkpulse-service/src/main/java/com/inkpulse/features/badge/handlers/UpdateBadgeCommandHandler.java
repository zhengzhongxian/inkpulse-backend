package com.inkpulse.features.badge.handlers;

import com.inkpulse.cache.CacheProperties;
import com.inkpulse.cache.ICacheService;
import com.inkpulse.constants.KeyConstants;
import com.inkpulse.constants.QueueConstants;
import com.inkpulse.constants.message.BadgeMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.*;
import com.inkpulse.features.badge.commands.UpdateBadgeCommand;
import com.inkpulse.models.response.badge.BadgeResponse;
import com.inkpulse.features.book.dto.SyncBookEditionMessage;
import com.inkpulse.repositories.BadgeRepository;
import com.inkpulse.repositories.BookEditionRepository;
import com.inkpulse.repositories.BookRepository;
import com.inkpulse.repositories.EditionBadgeRepository;
import com.inkpulse.service.outbox.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateBadgeCommandHandler implements Command.CommandHandler<UpdateBadgeCommand, BadgeResponse> {

        private final BadgeRepository badgeRepository;
        private final BookRepository bookRepository;
        private final EditionBadgeRepository editionBadgeRepository;
        private final OutboxPublisher outboxPublisher;
        private final ICacheService cacheService;
        private final CacheProperties cacheProperties;

        @Override
        @Transactional
        public BadgeResponse handle(UpdateBadgeCommand command) {
                log.info("Handling UpdateBadgeCommand: id={}, text={}", command.getId(), command.getText());

                Badge badge = badgeRepository.findById(command.getId())
                                .orElseThrow(() -> new BusinessValidationException(
                                                BadgeMessageConstants.BADGE_NOT_FOUND,
                                                BadgeMessageConstants.CODE_BADGE_NOT_FOUND));

                String oldText = badge.getText();
                String oldTextColor = badge.getTextColor();
                String oldBgColor = badge.getBgColor();

                badge.setText(command.getText());
                badge.setTextColor(command.getTextColor());
                badge.setBgColor(command.getBgColor());
                badge.setShape(command.getShape());

                Badge saved = badgeRepository.save(badge);

                // Publish outbox + evict cache for all affected book editions
                propagateBadgeChangeToBookEditions(saved);

                return BadgeResponse.builder()
                                .id(saved.getId())
                                .text(saved.getText())
                                .textColor(saved.getTextColor())
                                .bgColor(saved.getBgColor())
                                .shape(saved.getShape())
                                .build();
        }

        private void propagateBadgeChangeToBookEditions(Badge badge) {
                // 1. Books that have this badge as parent badge (Book.badge)
                List<Book> booksWithBadge = bookRepository.findByBadgeId(badge.getId());
                for (Book book : booksWithBadge) {
                        for (BookEdition edition : book.getEditions()) {
                                if (edition.isDeleted())
                                        continue;
                                publishEditionSync(edition, book, badge, null);
                                evictEditionCache(edition.getId());
                        }
                }

                // 2. Editions with this badge via editions_badges (N-N)
                List<EditionBadge> editionBadges = editionBadgeRepository.findByBadgeId(badge.getId());
                for (EditionBadge eb : editionBadges) {
                        if (eb.isDeleted())
                                continue;
                        BookEdition edition = eb.getEdition();
                        if (edition == null || edition.isDeleted())
                                continue;
                        Book book = edition.getBook();
                        if (book == null)
                                continue;

                        publishEditionSync(edition, book, book.getBadge(), edition);
                        evictEditionCache(edition.getId());
                }

                // 3. Invalidate badge editions Redis Set
                try {
                        String setKey = cacheProperties.buildKey(KeyConstants.SECTION_BADGE_EDITIONS,
                                        badge.getId().toString());
                        Set<String> editionIds = cacheService.smembers(setKey);
                        for (String edId : editionIds) {
                                try {
                                        cacheService.remove(cacheProperties
                                                        .buildKey(KeyConstants.SECTION_BOOK_EDITION_DETAIL, edId));
                                } catch (Exception ex) {
                                        log.error("Failed to evict edition detail cache for edition ID: {}", edId, ex);
                                }
                        }
                        cacheService.remove(setKey);
                } catch (Exception ex) {
                        log.error("Failed to invalidate badge editions set for badge ID: {}", badge.getId(), ex);
                }
        }

        private void publishEditionSync(BookEdition edition, Book book, Badge parentBadge, BookEdition activeEdition) {
                String authorNameJoined = book.getBookAuthors() != null
                                ? book.getBookAuthors().stream()
                                                .filter(ba -> ba.isActive() && ba.getAuthor() != null)
                                                .map(ba -> ba.getAuthor().getName())
                                                .reduce((a, b) -> a + ", " + b).orElse("")
                                : "";

                List<String> categorySlugs = book.getCategories() != null
                                ? book.getCategories().stream().map(Category::getSlug).toList()
                                : List.of();

                List<SyncBookEditionMessage.BadgeInfo> editionBadges = edition.getBadges() != null
                                ? edition.getBadges().stream()
                                                .filter(eb -> eb.getBadge() != null && !eb.isDeleted())
                                                .map(eb -> SyncBookEditionMessage.BadgeInfo.builder()
                                                                .text(eb.getBadge().getText())
                                                                .textColor(eb.getBadge().getTextColor())
                                                                .bgColor(eb.getBadge().getBgColor())
                                                                .shape(eb.getBadge().getShape())
                                                                .build())
                                                .toList()
                                : List.of();

                SyncBookEditionMessage msg = SyncBookEditionMessage.builder()
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
                                .badgeText(parentBadge != null ? parentBadge.getText() : null)
                                .badgeTextColor(parentBadge != null ? parentBadge.getTextColor() : null)
                                .badgeBgColor(parentBadge != null ? parentBadge.getBgColor() : null)
                                .active(book.isActive())
                                .deleted(book.isDeleted() || edition.isDeleted())
                                .categorySlugs(categorySlugs)
                                .imageUrls(edition.getImages().stream().map(EditionImage::getImageUrl).toList())
                                .badges(editionBadges)
                                .build();

                outboxPublisher.publish(
                                QueueConstants.SYNC_BOOK_EDITION,
                                msg,
                                "urn:message:InkPulse.Worker.Features.Book.Messages:SyncBookEditionMessage");
        }

        private void evictEditionCache(UUID editionId) {
                try {
                        cacheService.remove(cacheProperties.buildKey(KeyConstants.SECTION_BOOK_EDITION_DETAIL,
                                        editionId.toString()));
                } catch (Exception ex) {
                        log.error("Failed to evict edition detail cache for ID: {}", editionId, ex);
                }
        }
}