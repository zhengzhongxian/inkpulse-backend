package com.inkpulse.features.book.handlers;

import com.inkpulse.cache.CacheProperties;
import com.inkpulse.cache.ICacheService;
import com.inkpulse.constants.KeyConstants;
import com.inkpulse.constants.message.BookMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.BookEdition;
import com.inkpulse.features.book.commands.DeleteBookEditionCommand;
import com.inkpulse.features.book.dto.SyncBookEditionMessage;
import com.inkpulse.repositories.BookEditionRepository;
import com.inkpulse.constants.QueueConstants;
import com.inkpulse.service.outbox.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteBookEditionCommandHandler implements Command.CommandHandler<DeleteBookEditionCommand, Boolean> {

    private final BookEditionRepository bookEditionRepository;
    private final OutboxPublisher outboxPublisher;
    private final ICacheService cacheService;
    private final CacheProperties cacheProperties;

    @Override
    @Transactional
    public Boolean handle(DeleteBookEditionCommand cmd) {
        log.info("Handling DeleteBookEditionCommand for ID: {}", cmd.getId());

        if (cmd.getId() == null) {
            throw new BusinessValidationException(BookMessageConstants.BOOK_EDITION_NOT_FOUND,
                    BookMessageConstants.CODE_BOOK_EDITION_NOT_FOUND);
        }

        // 1. Retrieve existing BookEdition
        BookEdition edition = bookEditionRepository.findById(cmd.getId())
                .orElseThrow(() -> new BusinessValidationException(BookMessageConstants.BOOK_EDITION_NOT_FOUND,
                        BookMessageConstants.CODE_BOOK_EDITION_NOT_FOUND));

        // 2. Soft delete BookEdition in database
        edition.setDeleted(true);
        bookEditionRepository.save(edition);

        // 3. Sync delete to Elasticsearch
        SyncBookEditionMessage syncMsg = SyncBookEditionMessage.builder()
                .id(edition.getId())
                .bookId(edition.getBook().getId())
                .deleted(true)
                .build();

        outboxPublisher.publish(
                QueueConstants.SYNC_BOOK_EDITION,
                syncMsg,
                "urn:message:InkPulse.Worker.Features.Book.Messages:SyncBookEditionMessage");
        log.info("BookEdition deleted sync message published. Edition ID: {}", edition.getId());

        // 4. Evict Redis cache using the Redis Set
        try {
            CacheProperties.SectionConfig section = cacheProperties.getSections()
                    .get(KeyConstants.SECTION_BOOK_EDITION_DETAIL);
            if (section != null) {
                // Clear the deleted edition's cache key explicitly
                cacheService.remove(section.getKey() + edition.getId().toString());

                if (edition.getBook() != null) {
                    // Clear the Book ID fallback cache key explicitly
                    cacheService.remove(section.getKey() + edition.getBook().getId().toString());

                    String bookSetKey = cacheProperties.buildKey(KeyConstants.SECTION_BOOK_EDITIONS,
                            edition.getBook().getId().toString());
                    Set<String> editionIds = cacheService.smembers(bookSetKey);
                    if (editionIds != null && !editionIds.isEmpty()) {
                        for (String edId : editionIds) {
                            try {
                                cacheService.remove(section.getKey() + edId);
                            } catch (Exception ex) {
                                log.error("Failed to evict edition detail cache for ID: {}", edId, ex);
                            }
                        }
                    }
                    cacheService.remove(bookSetKey);
                }
                log.info("Evicted Redis cache key for deleted BookEdition ID: {}", edition.getId());
            }
        } catch (Exception e) {
            log.error("Failed to evict Redis cache key for deleted BookEdition ID: {}", edition.getId(), e);
        }

        return true;
    }
}
