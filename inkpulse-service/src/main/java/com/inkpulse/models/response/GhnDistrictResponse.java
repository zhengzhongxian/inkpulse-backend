package com.inkpulse.models.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GhnDistrictResponse {
    private Integer districtId;
    private Integer provinceId;
    private String districtName;
    private String districtCode;
    private Integer supportType;
}
