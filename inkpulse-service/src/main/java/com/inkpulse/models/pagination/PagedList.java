package com.inkpulse.models.pagination;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

@Getter
@Setter
@NoArgsConstructor
public class PagedList<T> {
    private int currentPage;
    private int totalPages;
    private int pageSize;
    private int totalCount;
    private List<T> items;

    public PagedList(List<T> items, int count, int pageNumber, int pageSize) {
        this.items = items;
        this.totalCount = count;
        this.currentPage = pageNumber;
        this.pageSize = pageSize;
        this.totalPages = pageSize > 0 ? (int) Math.ceil(count / (double) pageSize) : 0;
    }

    public boolean isHasPrevious() {
        return currentPage > 1;
    }

    public boolean isHasNext() {
        return currentPage < totalPages;
    }

    /**
     * Converts a Spring Data {@link Page} of entity objects into a {@link PagedList} of DTO objects
     * using the provided mapping function.
     */
    public static <TEntity, TDto> PagedList<TDto> fromPage(Page<TEntity> page, Function<TEntity, TDto> mapper) {
        List<TDto> mappedItems = page.getContent().stream()
                .map(mapper)
                .toList();
        // Spring's page.getNumber() is 0-indexed, so we add 1 to match 1-indexed pagination.
        return new PagedList<>(
                mappedItems,
                (int) page.getTotalElements(),
                page.getNumber() + 1,
                page.getSize()
        );
    }
}
