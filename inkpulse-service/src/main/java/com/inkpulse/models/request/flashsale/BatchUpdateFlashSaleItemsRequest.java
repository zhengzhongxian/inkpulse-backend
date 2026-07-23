package com.inkpulse.models.request.flashsale;

import com.inkpulse.features.flashsale.commands.UpdateFlashSaleItemsCommand.FlashSaleItemUpdatePayload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchUpdateFlashSaleItemsRequest {
    private List<FlashSaleItemUpdatePayload> items;
}
