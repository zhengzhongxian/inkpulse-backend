package com.inkpulse.models.request.flashsale;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddFlashSaleItemsRequest {
    private List<CreateFlashSaleRequest.FlashSaleItemPayload> items;
}
