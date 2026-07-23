package com.inkpulse.features.voucher.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.enums.UserVoucherStatus;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.pagination.PagedRequest;
import com.inkpulse.models.response.voucher.ExchangedVoucherResponse;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public class GetExchangedVouchersQuery extends PagedRequest implements Query<PagedList<ExchangedVoucherResponse>> {
    private UUID userId;
    private UserVoucherStatus status;
    private Boolean activeOnly;
}
