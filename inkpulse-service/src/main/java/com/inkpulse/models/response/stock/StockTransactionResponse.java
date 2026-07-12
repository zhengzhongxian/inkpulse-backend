package com.inkpulse.models.response.stock;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockTransactionResponse {
    private UUID id;
    private UUID editionId;
    private String isbn;
    private String bookTitle;
    private int delta;
    private String type;
    private String referenceCode;
    private String note;
    private UUID createdBy;
    private LocalDateTime createdAt;
}
