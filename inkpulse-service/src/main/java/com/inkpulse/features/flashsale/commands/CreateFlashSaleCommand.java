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

import com.inkpulse.models.request.flashsale.CreateFlashSaleRequest;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFlashSaleCommand implements Command<FlashSaleResponse> {
    private String name;
    private ZonedDateTime startDate;
    private ZonedDateTime endDate;
    private List<CreateFlashSaleRequest.FlashSaleItemPayload> items;
}
