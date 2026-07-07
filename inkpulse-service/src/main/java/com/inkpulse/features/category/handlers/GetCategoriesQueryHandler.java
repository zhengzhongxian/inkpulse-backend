package com.inkpulse.features.category.handlers;

import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.Category;
import com.inkpulse.models.response.category.CategoryResponse;
import com.inkpulse.features.category.queries.GetCategoriesQuery;
import com.inkpulse.repositories.CategoryRepository;
import com.inkpulse.cache.ICacheService;
import com.inkpulse.cache.CacheProperties;
import com.inkpulse.constants.KeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetCategoriesQueryHandler implements Query.QueryHandler<GetCategoriesQuery, List<CategoryResponse>> {

    private final CategoryRepository categoryRepository;
    private final ICacheService cacheService;
    private final CacheProperties cacheProperties;

    @Value("${" + KeyConstants.CATEGORY_LOCK_RETRY_TIMEOUT + ":5}")
    private int retryTimeoutSeconds;

    @Value("${" + KeyConstants.CATEGORY_LOCK_RETRY_INTERVAL + ":100}")
    private int retryIntervalMs;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> handle(GetCategoriesQuery query) {
        log.info("Handling GetCategoriesQuery");

        CacheProperties.SectionConfig section = cacheProperties.getSections().get(KeyConstants.SECTION_CATEGORIES);
        if (section == null) {
            throw new IllegalStateException("Cache section '" + KeyConstants.SECTION_CATEGORIES + "' is not configured in application.yml");
        }
        String cacheKey = section.getKey();
        String lockKey = "lock:" + cacheKey;
        Duration cacheTtl = Duration.ofMinutes(section.getTtl());
        Duration lockTtl = Duration.ofSeconds(10);

        CategoryResponse[] cached = cacheService.get(cacheKey, CategoryResponse[].class);
        if (cached != null) {
            log.debug("Categories cache hit");
            return Arrays.asList(cached);
        }

        log.debug("Categories cache miss. Attempting to acquire lock.");

        String lockValue = UUID.randomUUID().toString();

        if (cacheService.acquireLock(lockKey, lockValue, lockTtl, true, Duration.ofSeconds(retryTimeoutSeconds),
                Duration.ofMillis(retryIntervalMs))) {
            log.debug("Acquired lock for loading categories");
            try {
                cached = cacheService.get(cacheKey, CategoryResponse[].class);
                if (cached != null) {
                    log.debug("Categories cache hit on double-check");
                    return Arrays.asList(cached);
                }

                List<Category> categories = categoryRepository.findAll();
                List<CategoryResponse> resultList = categories.stream()
                        .map(this::mapToResponse)
                        .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                        .toList();

                cacheService.set(cacheKey, resultList.toArray(new CategoryResponse[0]), cacheTtl);
                log.debug("Categories loaded from database and saved to cache");
                return resultList;

            } catch (Exception e) {
                log.error("Error occurred while loading categories from database", e);
                throw e;
            } finally {
                boolean released = cacheService.releaseLock(lockKey, lockValue);
                log.debug("Released categories load lock: {}", released);
            }
        } else {
            log.debug("Failed to acquire lock. Falling back to direct database retrieval.");
            List<Category> categories = categoryRepository.findAll();
            return categories.stream()
                    .map(this::mapToResponse)
                    .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                    .toList();
        }
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .build();
    }
}
