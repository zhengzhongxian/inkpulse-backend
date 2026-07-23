package com.inkpulse.features.flashsale.queries;

import com.inkpulse.cqrs.Query;
import com.inkpulse.models.response.flashsale.FlashSaleItemResponse;

import java.util.List;

public class GetActiveFlashSalesQuery implements Query<List<FlashSaleItemResponse>> {
}
