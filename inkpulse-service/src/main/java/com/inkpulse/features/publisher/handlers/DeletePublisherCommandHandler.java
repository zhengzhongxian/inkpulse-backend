package com.inkpulse.features.publisher.handlers;

import com.inkpulse.cache.CacheProperties;
import com.inkpulse.cache.ICacheService;
import com.inkpulse.constants.KeyConstants;
import com.inkpulse.constants.QueueConstants;
import com.inkpulse.constants.message.PublisherMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.Publisher;
import com.inkpulse.features.publisher.commands.DeletePublisherCommand;
import com.inkpulse.features.publisher.dto.SyncPublisherNameMessage;
import com.inkpulse.repositories.PublisherRepository;
import com.inkpulse.service.outbox.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeletePublisherCommandHandler implements Command.CommandHandler<DeletePublisherCommand, Boolean> {

    private final PublisherRepository publisherRepository;
    private final OutboxPublisher outboxPublisher;
    private final ICacheService cacheService;
    private final CacheProperties cacheProperties;

    @Override
    @Transactional
    public Boolean handle(DeletePublisherCommand command) {
        log.info("Handling DeletePublisherCommand: id={}", command.getId());

        Publisher pub = publisherRepository.findById(command.getId())
                .orElseThrow(() -> new BusinessValidationException(
                        PublisherMessageConstants.PUBLISHER_NOT_FOUND,
                        PublisherMessageConstants.CODE_PUBLISHER_NOT_FOUND));

        pub.setDeleted(true);
        publisherRepository.save(pub);

        // 1. Invalidate publisher editions Redis Set & detail caches
        try {
            String setKey = cacheProperties.buildKey(KeyConstants.SECTION_PUBLISHER_EDITIONS, pub.getId().toString());
            Set<String> editionIds = cacheService.smembers(setKey);
            if (editionIds != null && !editionIds.isEmpty()) {
                for (String edId : editionIds) {
                    try {
                        cacheService.remove(cacheProperties.buildKey(KeyConstants.SECTION_BOOK_EDITION_DETAIL, edId));
                    } catch (Exception ex) {
                        log.error("Failed to evict edition detail cache for edition ID: {}", edId, ex);
                    }
                }
            }
            cacheService.remove(setKey);
        } catch (Exception ex) {
            log.error("Failed to invalidate publisher editions set for publisher ID: {}", pub.getId(), ex);
        }

        // 2. Publish SyncPublisherNameMessage with isDeleted = true for ELS sync
        try {
            SyncPublisherNameMessage msg = SyncPublisherNameMessage.builder()
                    .id(pub.getId())
                    .name(null)
                    .isDeleted(true)
                    .build();

            outboxPublisher.publish(
                    QueueConstants.SYNC_PUBLISHER_NAME,
                    msg,
                    "urn:message:InkPulse.Worker.Features.Book.Messages:SyncPublisherNameMessage");
            log.info("Published SyncPublisherNameMessage for deleted publisher: id={}", pub.getId());
        } catch (Exception ex) {
            log.error("Failed to publish SyncPublisherNameMessage for deleted publisher ID: {}", pub.getId(), ex);
        }

        return true;
    }
}