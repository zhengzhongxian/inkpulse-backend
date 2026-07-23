package com.inkpulse.features.voucher.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.enums.VoucherDiscountType;
import com.inkpulse.entities.enums.VoucherTargetType;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.pagination.PagedRequest;
import com.inkpulse.models.response.voucher.VoucherResponse;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Getter
@Setter
public class GetInternalVouchersQuery extends PagedRequest implements Query<PagedList<VoucherResponse>> {
    private VoucherDiscountType discountType;
    private VoucherTargetType targetType;
    private BigDecimal minDiscountValue;
    private BigDecimal maxDiscountValue;
    private Integer minMaxUses;
    private Integer maxMaxUses;
    private Integer minCoinCost;
    private Integer maxCoinCost;
    private LocalDateTime createdFrom;
    private LocalDateTime createdTo;
    private ZonedDateTime startDateFrom;
    private ZonedDateTime startDateTo;
    private ZonedDateTime endDateFrom;
    private ZonedDateTime endDateTo;
    private Boolean isActive;
}
