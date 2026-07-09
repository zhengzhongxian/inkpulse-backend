package com.inkpulse.models.response.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmPackOrderResponse {
    private UUID orderId;
    private String orderCode;
    private String ghnOrderCode;
    private String message;
}
