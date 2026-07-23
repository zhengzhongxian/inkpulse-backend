package com.inkpulse.features.category.handlers;

import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.Category;
import com.inkpulse.models.response.category.CategoryResponse;
import com.inkpulse.features.category.queries.GetInternalCategoriesQuery;
import com.inkpulse.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetInternalCategoriesQueryHandler
        implements Query.QueryHandler<GetInternalCategoriesQuery, List<CategoryResponse>> {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> handle(GetInternalCategoriesQuery query) {
        log.info("Handling GetInternalCategoriesQuery");
        return categoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .createdAt(category.getCreatedAt())
                .build();
    }
}