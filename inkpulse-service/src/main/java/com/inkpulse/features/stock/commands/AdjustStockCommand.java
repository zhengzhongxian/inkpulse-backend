package com.inkpulse.features.stock.commands;

import com.inkpulse.cqrs.Command;
import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustStockCommand implements Command<Void> {
    private UUID editionId;
    private int newQuantity;
    private String note;
    private UUID adminUserId;
}
