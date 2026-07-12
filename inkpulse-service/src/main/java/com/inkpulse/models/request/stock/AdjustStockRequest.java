package com.inkpulse.models.request.stock;

import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustStockRequest {
    private UUID editionId;
    private int newQuantity;
    private String note;
}
