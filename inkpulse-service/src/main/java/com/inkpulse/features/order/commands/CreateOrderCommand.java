package com.inkpulse.features.order.commands;

import com.inkpulse.cqrs.Command;
import com.inkpulse.models.response.order.CreateOrderResponse;
import com.inkpulse.models.request.order.OrderItemRequest;
import lombok.*;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderCommand implements Command<CreateOrderResponse> {
    private UUID userId;
    private UUID addressId;
    private Integer provinceId;
    private Integer districtId;
    private String wardCode;
    private String streetAddress;
    private String addressLabel;
    private String recipientPhone;
    private String receiverName;
    private String paymentMethod;
    private String note;
    private List<OrderItemRequest> items;
    private String source;
    private List<UUID> cartItemIds;
}
