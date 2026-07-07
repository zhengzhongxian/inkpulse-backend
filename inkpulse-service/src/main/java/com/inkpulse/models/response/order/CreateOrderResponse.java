package com.inkpulse.models.response.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderResponse {
    private java.util.UUID orderId;
    private String orderCode;
    private String orderStatus;
    private String paymentStatus;
    private String checkoutUrl;
    private String message;
}
