package com.inkpulse.models.request.flashsale;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFlashSaleRequest {
    private String name;
    private Boolean isActive;
    private ZonedDateTime startDate;
    private ZonedDateTime endDate;
    private List<CreateFlashSaleRequest.FlashSaleItemPayload> items;
}
