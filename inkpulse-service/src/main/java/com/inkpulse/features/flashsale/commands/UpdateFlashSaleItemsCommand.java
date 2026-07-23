package com.inkpulse.features.flashsale.commands;

import com.inkpulse.cqrs.Command;
import com.inkpulse.models.response.flashsale.FlashSaleItemResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFlashSaleItemsCommand implements Command<List<FlashSaleItemResponse>> {
    private UUID flashSaleId;
    private List<FlashSaleItemUpdatePayload> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FlashSaleItemUpdatePayload {
        private UUID flashSaleItemId;
        private BigDecimal discountAmount;
        private Integer flashSaleStock;
    }
}
