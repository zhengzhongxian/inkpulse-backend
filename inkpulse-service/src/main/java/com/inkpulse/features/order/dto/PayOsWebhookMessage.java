package com.inkpulse.features.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayOsWebhookMessage {
    private String orderCode;
    private String paymentLinkId;
    private int amount;
    private String description;
    private String code;
    private boolean success;
}
