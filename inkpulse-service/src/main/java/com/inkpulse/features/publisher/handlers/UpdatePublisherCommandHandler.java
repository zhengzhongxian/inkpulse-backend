package com.inkpulse.features.publisher.handlers;

import com.inkpulse.cache.CacheProperties;
import com.inkpulse.cache.ICacheService;
import com.inkpulse.constants.KeyConstants;
import com.inkpulse.constants.QueueConstants;
import com.inkpulse.constants.message.PublisherMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.Publisher;
import com.inkpulse.features.publisher.commands.UpdatePublisherCommand;
import com.inkpulse.models.response.publisher.PublisherResponse;
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
public class UpdatePublisherCommandHandler
                implements Command.CommandHandler<UpdatePublisherCommand, PublisherResponse> {

        private final PublisherRepository publisherRepository;
        private final OutboxPublisher outboxPublisher;
        private final ICacheService cacheService;
        private final CacheProperties cacheProperties;

        @Override
        @Transactional
        public PublisherResponse handle(UpdatePublisherCommand command) {
                log.info("Handling UpdatePublisherCommand: id={}, name={}", command.getId(), command.getName());

                if (command.getName() == null || command.getName().isBlank()) {
                        throw new BusinessValidationException(
                                        PublisherMessageConstants.PUBLISHER_NAME_EMPTY,
                                        PublisherMessageConstants.CODE_PUBLISHER_NAME_EMPTY);
                }

                Publisher pub = publisherRepository.findById(command.getId())
                                .orElseThrow(() -> new BusinessValidationException(
                                                PublisherMessageConstants.PUBLISHER_NOT_FOUND,
                                                PublisherMessageConstants.CODE_PUBLISHER_NOT_FOUND));

                pub.setName(command.getName().trim());
                pub.setAddress(command.getAddress() != null ? command.getAddress().trim() : null);

                Publisher saved = publisherRepository.save(pub);

                propagatePublisherChangeToBookEditions(saved);

                return PublisherResponse.builder()
                                .id(saved.getId())
                                .name(saved.getName())
                                .address(saved.getAddress())
                                .build();
        }

        private void propagatePublisherChangeToBookEditions(Publisher publisher) {
                // 1. Invalidate publisher editions Redis Set & detail caches
                try {
                        String setKey = cacheProperties.buildKey(KeyConstants.SECTION_PUBLISHER_EDITIONS,
                                        publisher.getId().toString());
                        Set<String> editionIds = cacheService.smembers(setKey);
                        if (editionIds != null && !editionIds.isEmpty()) {
                                for (String edId : editionIds) {
                                        try {
                                                cacheService.remove(cacheProperties
                                                                .buildKey(KeyConstants.SECTION_BOOK_EDITION_DETAIL, edId));
                                        } catch (Exception ex) {
                                                log.error("Failed to evict edition detail cache for edition ID: {}", edId, ex);
                                        }
                                }
                        }
                        cacheService.remove(setKey);
                } catch (Exception ex) {
                        log.error("Failed to invalidate publisher editions set for publisher ID: {}", publisher.getId(),
                                        ex);
                }

                // 2. Publish a single UpdatePublisherNameMessage to RabbitMQ for ELS sync
                try {
                        SyncPublisherNameMessage msg = SyncPublisherNameMessage.builder()
                                        .id(publisher.getId())
                                        .name(publisher.getName())
                                        .isDeleted(false)
                                        .build();

                        outboxPublisher.publish(
                                        QueueConstants.SYNC_PUBLISHER_NAME,
                                        msg,
                                        "urn:message:InkPulse.Worker.Features.Book.Messages:SyncPublisherNameMessage");
                        log.info("Published SyncPublisherNameMessage for publisher: id={}, name={}", publisher.getId(), publisher.getName());
                } catch (Exception ex) {
                        log.error("Failed to publish SyncPublisherNameMessage for publisher ID: {}", publisher.getId(), ex);
                }
        }
}