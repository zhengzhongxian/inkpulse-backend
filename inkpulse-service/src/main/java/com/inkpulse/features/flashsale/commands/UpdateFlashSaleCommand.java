package com.inkpulse.features.flashsale.commands;

import com.inkpulse.cqrs.Command;
import com.inkpulse.models.response.flashsale.FlashSaleResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFlashSaleCommand implements Command<FlashSaleResponse> {
    private UUID flashSaleId;
    private String name;
    private Boolean isActive;
    private ZonedDateTime startDate;
    private ZonedDateTime endDate;
}
