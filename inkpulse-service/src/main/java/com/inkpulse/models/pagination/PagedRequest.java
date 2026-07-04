package com.inkpulse.models.pagination;

import com.inkpulse.constants.AppConstants;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@Setter
public class PagedRequest {
    private int pageNumber = 1;
    private int pageSize = AppConstants.Pagination.DEFAULT_PAGE_SIZE;
    private String searchKeyword;
    private String sortBy;
    private String sortDirection;

    public void setPageSize(int pageSize) {
        if (pageSize > AppConstants.Pagination.MAX_PAGE_SIZE) {
            this.pageSize = AppConstants.Pagination.MAX_PAGE_SIZE;
        } else if (pageSize < AppConstants.Pagination.MIN_PAGE_SIZE) {
            this.pageSize = AppConstants.Pagination.MIN_PAGE_SIZE;
        } else {
            this.pageSize = pageSize;
        }
    }

    public Pageable toPageable() {
        int pageIndex = Math.max(0, this.pageNumber - 1);
        Sort sort = Sort.unsorted();

        if (this.sortBy != null && !this.sortBy.trim().isEmpty()) {
            Sort.Direction direction = "desc".equalsIgnoreCase(this.sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
            sort = Sort.by(direction, this.sortBy.trim());
        }

        return PageRequest.of(pageIndex, this.pageSize, sort);
    }
}
