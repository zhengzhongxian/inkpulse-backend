package com.inkpulse.service.ghn;

import com.inkpulse.models.request.ghn.GhnCalculateFeeRequest;
import com.inkpulse.models.response.ghn.GhnCalculateFeeResponse;

public interface IGhnShippingService {
    GhnCalculateFeeResponse calculateShippingFee(GhnCalculateFeeRequest request);
}
