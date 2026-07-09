package com.inkpulse.features.order.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.pagination.PagedRequest;
import com.inkpulse.models.response.order.OrderSummaryResponse;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GetInternalOrdersQuery extends PagedRequest implements Query<PagedList<OrderSummaryResponse>> {
    private String status;
}
