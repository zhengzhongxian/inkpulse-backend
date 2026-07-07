package com.inkpulse.features.cart.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.pagination.PagedRequest;
import com.inkpulse.models.response.cart.CartItemResponse;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class GetMyCartQuery extends PagedRequest implements Query<PagedList<CartItemResponse>> {
    private UUID userId;

    public GetMyCartQuery() {
        super();
        this.setSortBy("createdAt");
        this.setSortDirection("desc");
    }

    public GetMyCartQuery(UUID userId, int pageNumber, int pageSize) {
        this();
        this.userId = userId;
        this.setPageNumber(pageNumber);
        this.setPageSize(pageSize);
    }
}
