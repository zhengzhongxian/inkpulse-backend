package com.inkpulse.features.order.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.models.response.order.OrderLogResponse;
import java.util.List;

public record GetOrderLogsQuery(String orderCode) implements Query<List<OrderLogResponse>> {
}
