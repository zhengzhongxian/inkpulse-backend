package com.inkpulse.features.order.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.pagination.PagedRequest;
import com.inkpulse.models.response.order.OrderSummaryResponse;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class GetMyOrdersQuery extends PagedRequest implements Query<PagedList<OrderSummaryResponse>> {
    private UUID userId;

    public GetMyOrdersQuery() {
        super();
        this.setSortBy("createdAt");
        this.setSortDirection("desc");
    }

    public GetMyOrdersQuery(UUID userId, int pageNumber, int pageSize) {
        this();
        this.userId = userId;
        this.setPageNumber(pageNumber);
        this.setPageSize(pageSize);
    }
}
