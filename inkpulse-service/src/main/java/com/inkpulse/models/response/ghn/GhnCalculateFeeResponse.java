package com.inkpulse.models.response.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GhnCalculateFeeResponse {
    private int code;
    private String message;
    private FeeData data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeeData {
        private BigDecimal total;
        
        @JsonProperty("service_fee")
        private BigDecimal serviceFee;
        
        @JsonProperty("insurance_fee")
        private BigDecimal insuranceFee;
        
        @JsonProperty("coupon_value")
        private BigDecimal couponValue;
        
        @JsonProperty("cod_failed_fee")
        private BigDecimal codFailedFee;
    }
}
