package com.inkpulse.features.book.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.models.response.book.BookResponse;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.pagination.PagedRequest;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class GetInternalBooksQuery extends PagedRequest implements Query<PagedList<BookResponse>> {
    private String categorySlug;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String coverType;
    private String authorName;
    private Boolean active;
    private String startDate;
    private String endDate;

    public GetInternalBooksQuery(int pageNumber, int pageSize, String searchKeyword, String categorySlug,
                                 String sortBy, String sortDirection, BigDecimal minPrice, BigDecimal maxPrice,
                                 String coverType, String authorName, Boolean active, String startDate, String endDate) {
        this.setPageNumber(pageNumber);
        this.setPageSize(pageSize);
        this.setSearchKeyword(searchKeyword);
        this.setSortBy(sortBy);
        this.setSortDirection(sortDirection);
        this.categorySlug = categorySlug;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.coverType = coverType;
        this.authorName = authorName;
        this.active = active;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
