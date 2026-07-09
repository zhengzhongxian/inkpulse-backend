package com.inkpulse.features.order.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.models.response.order.OrderDetailResponse;
import java.util.UUID;

public record GetInternalOrderDetailQuery(UUID orderId) implements Query<OrderDetailResponse> {
}
