package com.inkpulse.features.voucher.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.enums.VoucherTargetType;
import com.inkpulse.entities.enums.VoucherDiscountType;
import com.inkpulse.models.pagination.PagedList;
import com.inkpulse.models.pagination.PagedRequest;
import com.inkpulse.models.response.voucher.PublicVoucherResponse;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class GetPublicVouchersQuery extends PagedRequest implements Query<PagedList<PublicVoucherResponse>> {
    private VoucherTargetType targetType;
    private Integer maxCoinCost;
    
    // Additional filters
    private Boolean suitableOnly;
    private UUID userId;
    private VoucherDiscountType discountType;
    private BigDecimal minDiscountValue;
    private BigDecimal maxDiscountValue;
    private BigDecimal minMinOrderValue;
    private BigDecimal maxMinOrderValue;
}
