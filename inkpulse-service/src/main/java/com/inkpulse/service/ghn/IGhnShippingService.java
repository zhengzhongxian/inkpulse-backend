package com.inkpulse.service.ghn;

import com.inkpulse.models.request.ghn.GhnCalculateFeeRequest;
import com.inkpulse.models.response.ghn.GhnCalculateFeeResponse;

public interface IGhnShippingService {
    GhnCalculateFeeResponse calculateShippingFee(GhnCalculateFeeRequest request);
    String generatePrintToken(String ghnOrderCode);
    void updateShippingOrder(String ghnOrderCode, String note, String requiredNote, Integer weight, Integer length, Integer width, Integer height);
}
