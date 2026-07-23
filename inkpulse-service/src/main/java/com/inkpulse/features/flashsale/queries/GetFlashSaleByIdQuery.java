package com.inkpulse.features.flashsale.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.models.response.flashsale.FlashSaleDetailResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetFlashSaleByIdQuery implements Query<FlashSaleDetailResponse> {
    private UUID flashSaleId;
}
