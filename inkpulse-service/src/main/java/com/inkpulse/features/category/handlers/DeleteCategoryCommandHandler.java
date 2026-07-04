package com.inkpulse.features.category.handlers;

import com.inkpulse.cache.CacheProperties;
import com.inkpulse.cache.ICacheService;
import com.inkpulse.constants.KeyConstants;
import com.inkpulse.constants.QueueConstants;
import com.inkpulse.constants.message.CategoryMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.Category;
import com.inkpulse.features.category.commands.DeleteCategoryCommand;
import com.inkpulse.features.category.dto.SyncCategorySlugMessage;
import com.inkpulse.repositories.CategoryRepository;
import com.inkpulse.service.outbox.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeleteCategoryCommandHandler implements Command.CommandHandler<DeleteCategoryCommand, Boolean> {

    private final CategoryRepository categoryRepository;
    private final OutboxPublisher outboxPublisher;
    private final ICacheService cacheService;
    private final CacheProperties cacheProperties;

    @Override
    @Transactional
    public Boolean handle(DeleteCategoryCommand command) {
        log.info("Handling DeleteCategoryCommand: id={}", command.getId());

        Category category = categoryRepository.findById(command.getId())
                .orElseThrow(() -> new BusinessValidationException(
                        CategoryMessageConstants.CATEGORY_NOT_FOUND,
                        CategoryMessageConstants.CODE_CATEGORY_NOT_FOUND));

        String categorySlug = category.getSlug();
        category.setDeleted(true);
        categoryRepository.save(category);

        // 1. Evict public categories cache and linked book editions
        evictCategoriesCache(categorySlug);

        // 2. Publish SyncCategorySlugMessage with isDeleted = true for ELS sync
        try {
            SyncCategorySlugMessage msg = SyncCategorySlugMessage.builder()
                    .id(category.getId())
                    .oldSlug(categorySlug)
                    .newSlug(null)
                    .isDeleted(true)
                    .build();

            outboxPublisher.publish(
                    QueueConstants.SYNC_CATEGORY_SLUG,
                    msg,
                    "urn:message:InkPulse.Worker.Features.Book.Messages:SyncCategorySlugMessage");
            log.info("Published SyncCategorySlugMessage for deleted category: id={}, slug={}", category.getId(), categorySlug);
        } catch (Exception ex) {
            log.error("Failed to publish SyncCategorySlugMessage for deleted category ID: {}", category.getId(), ex);
        }

        return true;
    }

    private void evictCategoriesCache(String categorySlug) {
        try {
            CacheProperties.SectionConfig section = cacheProperties.getSections()
                    .get(KeyConstants.SECTION_CATEGORIES);
            if (section != null) {
                cacheService.remove(section.getKey());
                log.debug("Evicted public categories cache");
            }

            if (categorySlug != null) {
                String setKey = cacheProperties.buildKey(KeyConstants.SECTION_CATEGORY_EDITIONS, categorySlug);
                java.util.Set<String> editionIds = cacheService.smembers(setKey);
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
            }
        } catch (Exception e) {
            log.error("Failed to evict categories cache", e);
        }
    }
}