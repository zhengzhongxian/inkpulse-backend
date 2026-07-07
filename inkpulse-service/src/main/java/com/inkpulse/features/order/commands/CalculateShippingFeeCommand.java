package com.inkpulse.features.order.commands;

import com.inkpulse.cqrs.Command;
import com.inkpulse.models.response.order.CalculateShippingFeeResponse;
import com.inkpulse.models.request.order.OrderItemRequest;
import lombok.*;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculateShippingFeeCommand implements Command<CalculateShippingFeeResponse> {
    private UUID userId;
    private int toDistrictId;
    private String toWardCode;
    private List<OrderItemRequest> items;
}
