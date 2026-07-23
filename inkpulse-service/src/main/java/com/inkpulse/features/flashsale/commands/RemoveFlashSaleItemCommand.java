package com.inkpulse.features.flashsale.commands;

import com.inkpulse.cqrs.Command;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoveFlashSaleItemCommand implements Command<Void> {
    private UUID flashSaleId;
    private UUID flashSaleItemId;
}
