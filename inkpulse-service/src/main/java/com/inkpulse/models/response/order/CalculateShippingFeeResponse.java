package com.inkpulse.models.response.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculateShippingFeeResponse {
    private BigDecimal total;
    private BigDecimal serviceFee;
    private BigDecimal insuranceFee;
    private BigDecimal couponValue;
    private BigDecimal codFailedFee;
}
