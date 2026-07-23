package com.inkpulse.models.response.flashsale;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FlashSaleDetailResponse extends FlashSaleResponse {
    private List<FlashSaleItemResponse> items;
}
