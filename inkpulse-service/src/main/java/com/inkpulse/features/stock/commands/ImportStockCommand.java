package com.inkpulse.features.stock.commands;

import com.inkpulse.cqrs.Command;
import lombok.*;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportStockCommand implements Command<Void> {
    private UUID editionId;
    private int quantity;
    private String note;
    private UUID adminUserId;
}
