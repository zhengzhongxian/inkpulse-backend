package com.inkpulse.features.flashsale.commands;

import com.inkpulse.cqrs.Command;
import com.inkpulse.models.request.flashsale.CreateFlashSaleRequest.FlashSaleItemPayload;
import com.inkpulse.models.response.flashsale.FlashSaleItemResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddFlashSaleItemsCommand implements Command<List<FlashSaleItemResponse>> {
    private UUID flashSaleId;
    private List<FlashSaleItemPayload> items;
}
