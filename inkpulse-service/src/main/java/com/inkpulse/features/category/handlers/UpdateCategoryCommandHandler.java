package com.inkpulse.features.category.handlers;

import com.inkpulse.cache.CacheProperties;
import com.inkpulse.cache.ICacheService;
import com.inkpulse.constants.KeyConstants;
import com.inkpulse.constants.QueueConstants;
import com.inkpulse.constants.message.CategoryMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.Category;
import com.inkpulse.features.category.commands.UpdateCategoryCommand;
import com.inkpulse.features.category.dto.SyncCategorySlugMessage;
import com.inkpulse.models.response.CategoryResponse;
import com.inkpulse.repositories.CategoryRepository;
import com.inkpulse.service.outbox.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateCategoryCommandHandler implements Command.CommandHandler<UpdateCategoryCommand, CategoryResponse> {

    private final CategoryRepository categoryRepository;
    private final OutboxPublisher outboxPublisher;
    private final ICacheService cacheService;
    private final CacheProperties cacheProperties;

    @Override
    @Transactional
    public CategoryResponse handle(UpdateCategoryCommand command) {
        log.info("Handling UpdateCategoryCommand: id={}, name={}, slug={}", command.getId(), command.getName(),
                command.getSlug());

        Category category = categoryRepository.findById(command.getId())
                .orElseThrow(() -> new BusinessValidationException(
                        CategoryMessageConstants.CATEGORY_NOT_FOUND,
                        CategoryMessageConstants.CODE_CATEGORY_NOT_FOUND));

        // Check slug uniqueness (excluding self)
        categoryRepository.findAll().stream()
                .filter(c -> !c.getId().equals(command.getId()) && command.getSlug().equalsIgnoreCase(c.getSlug()))
                .findAny()
                .ifPresent(c -> {
                    throw new BusinessValidationException(
                            CategoryMessageConstants.SLUG_ALREADY_EXISTS,
                            CategoryMessageConstants.CODE_SLUG_ALREADY_EXISTS);
                });

        String oldSlug = category.getSlug();
        category.setName(command.getName());
        category.setSlug(command.getSlug());

        if (command.getParentId() != null) {
            Category parent = categoryRepository.findById(command.getParentId())
                    .orElseThrow(() -> new BusinessValidationException(
                            CategoryMessageConstants.CATEGORY_NOT_FOUND,
                            CategoryMessageConstants.CODE_CATEGORY_NOT_FOUND));
            // Prevent circular reference
            if (parent.getId().equals(category.getId())) {
                throw new BusinessValidationException(
                        "Cannot set parent to self",
                        "CATEGORY_SELF_PARENT");
            }
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        Category saved = categoryRepository.save(category);

        // Evict public categories cache and linked book editions cache
        evictCategoriesCache(oldSlug);
        if (oldSlug != null && !oldSlug.equals(saved.getSlug())) {
            evictCategoriesCache(saved.getSlug());
        }

        // Publish ELS sync if slug changed
        if (oldSlug != null && !oldSlug.equals(saved.getSlug())) {
            try {
                SyncCategorySlugMessage msg = SyncCategorySlugMessage.builder()
                        .id(saved.getId())
                        .oldSlug(oldSlug)
                        .newSlug(saved.getSlug())
                        .isDeleted(false)
                        .build();

                outboxPublisher.publish(
                        QueueConstants.SYNC_CATEGORY_SLUG,
                        msg,
                        "urn:message:InkPulse.Worker.Features.Book.Messages:SyncCategorySlugMessage");
                log.info("Published SyncCategorySlugMessage: categoryId={}, oldSlug={}, newSlug={}", saved.getId(), oldSlug, saved.getSlug());
            } catch (Exception ex) {
                log.error("Failed to publish SyncCategorySlugMessage for category ID: {}", saved.getId(), ex);
            }
        }

        return CategoryResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .slug(saved.getSlug())
                .parentId(saved.getParent() != null ? saved.getParent().getId() : null)
                .build();
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