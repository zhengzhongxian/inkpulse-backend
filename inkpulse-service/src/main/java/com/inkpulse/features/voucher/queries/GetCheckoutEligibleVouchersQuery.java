package com.inkpulse.features.voucher.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.models.request.order.OrderItemRequest;
import com.inkpulse.models.response.voucher.CheckoutEligibleVoucherResponse;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class GetCheckoutEligibleVouchersQuery implements Query<List<CheckoutEligibleVoucherResponse>> {
    private UUID userId;
    private List<OrderItemRequest> items;
}
