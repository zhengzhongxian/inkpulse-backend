package com.inkpulse.models.request.flashsale;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFlashSaleRequest {
    private String name;
    private ZonedDateTime startDate;
    private ZonedDateTime endDate;
    private List<FlashSaleItemPayload> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlashSaleItemPayload {
        private UUID bookEditionId;
        private BigDecimal discountAmount;
        private Integer flashSaleStock;
    }
}
