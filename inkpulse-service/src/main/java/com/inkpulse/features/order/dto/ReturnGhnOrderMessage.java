package com.inkpulse.features.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnGhnOrderMessage {
    private String orderCode;
    private String ghnOrderCode;
}
