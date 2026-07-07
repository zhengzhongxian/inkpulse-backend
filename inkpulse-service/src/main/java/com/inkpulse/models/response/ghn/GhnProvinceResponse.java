package com.inkpulse.models.response.ghn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GhnProvinceResponse {
    private Integer provinceId;
    private String provinceName;
    private String provinceCode;
}
