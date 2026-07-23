package com.inkpulse.features.flashsale.dto;

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
public class SyncFlashSaleToElsMessage {
    private UUID bookEditionId;
    private BigDecimal flashSalePrice;
    private String flashSaleItemId;
}
