package com.inkpulse.features.order.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.pagination.PagedRequest;
import com.inkpulse.models.response.order.OrderSummaryResponse;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class GetInternalOrdersQuery extends PagedRequest implements Query<PagedList<OrderSummaryResponse>> {
    private String status;
    private String startDate;
    private String endDate;
    private String paymentMethod;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private Boolean hasVoucher;
    private Boolean hasFlashSale;
}
