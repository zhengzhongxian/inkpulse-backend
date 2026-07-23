package com.inkpulse.features.flashsale.commands;

import com.inkpulse.cqrs.Command;
import com.inkpulse.models.response.flashsale.FlashSaleItemResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddFlashSaleItemCommand implements Command<FlashSaleItemResponse> {
    private UUID flashSaleId;
    private UUID bookEditionId;
    private BigDecimal discountAmount;
    private Integer flashSaleStock;
}
