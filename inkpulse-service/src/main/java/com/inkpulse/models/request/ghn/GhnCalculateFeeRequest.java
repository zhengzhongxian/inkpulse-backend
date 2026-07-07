package com.inkpulse.models.request.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GhnCalculateFeeRequest {
    @JsonProperty("from_district_id")
    private Integer fromDistrictId;

    @JsonProperty("from_ward_code")
    private String fromWardCode;

    @JsonProperty("service_id")
    private Integer serviceId;

    @JsonProperty("service_type_id")
    @Builder.Default
    private Integer serviceTypeId = 2;

    @JsonProperty("to_district_id")
    private int toDistrictId;

    @JsonProperty("to_ward_code")
    private String toWardCode;

    private int weight;
    private int length;
    private int width;
    private int height;

    @JsonProperty("insurance_value")
    private int insuranceValue;

    @JsonProperty("cod_failed_amount")
    @Builder.Default
    private int codFailedAmount = 2000;

    private String coupon;
    private List<GhnShippingItem> items;
}
