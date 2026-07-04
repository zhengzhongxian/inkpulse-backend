package com.inkpulse.features.category.handlers;

import com.inkpulse.cache.CacheProperties;
import com.inkpulse.cache.ICacheService;
import com.inkpulse.constants.KeyConstants;
import com.inkpulse.constants.message.CategoryMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.Category;
import com.inkpulse.features.category.commands.CreateCategoryCommand;
import com.inkpulse.models.response.CategoryResponse;
import com.inkpulse.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateCategoryCommandHandler implements Command.CommandHandler<CreateCategoryCommand, CategoryResponse> {

    private final CategoryRepository categoryRepository;
    private final ICacheService cacheService;
    private final CacheProperties cacheProperties;

    @Override
    @Transactional
    public CategoryResponse handle(CreateCategoryCommand command) {
        log.info("Handling CreateCategoryCommand: name={}, slug={}", command.getName(), command.getSlug());

        // Check slug uniqueness
        categoryRepository.findAll().stream()
                .filter(c -> command.getSlug().equalsIgnoreCase(c.getSlug()))
                .findAny()
                .ifPresent(c -> {
                    throw new BusinessValidationException(
                            CategoryMessageConstants.SLUG_ALREADY_EXISTS,
                            CategoryMessageConstants.CODE_SLUG_ALREADY_EXISTS);
                });

        Category parent = null;
        if (command.getParentId() != null) {
            parent = categoryRepository.findById(command.getParentId())
                    .orElseThrow(() -> new BusinessValidationException(
                            CategoryMessageConstants.CATEGORY_NOT_FOUND,
                            CategoryMessageConstants.CODE_CATEGORY_NOT_FOUND));
        }

        Category category = Category.builder()
                .name(command.getName())
                .slug(command.getSlug())
                .parent(parent)
                .build();

        Category saved = categoryRepository.save(category);

        // Evict public categories cache
        evictCategoriesCache();

        return CategoryResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .slug(saved.getSlug())
                .parentId(saved.getParent() != null ? saved.getParent().getId() : null)
                .build();
    }

    private void evictCategoriesCache() {
        try {
            CacheProperties.SectionConfig section = cacheProperties.getSections()
                    .get(KeyConstants.SECTION_CATEGORIES);
            if (section != null) {
                cacheService.remove(section.getKey());
                log.debug("Evicted public categories cache after create");
            }
        } catch (Exception e) {
            log.error("Failed to evict categories cache after create", e);
        }
    }
}