package com.inkpulse.features.refund.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.pagination.PagedRequest;
import com.inkpulse.models.response.refund.RefundRequestResponse;

public class GetRefundRequestsQuery extends PagedRequest implements Query<PagedList<RefundRequestResponse>> {
    private String status;
    private String startDate;
    private String endDate;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }
}
