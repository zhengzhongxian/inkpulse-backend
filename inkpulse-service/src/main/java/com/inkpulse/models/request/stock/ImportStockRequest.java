package com.inkpulse.models.request.stock;

import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportStockRequest {
    private UUID editionId;
    private int quantity;
    private String note;
}
