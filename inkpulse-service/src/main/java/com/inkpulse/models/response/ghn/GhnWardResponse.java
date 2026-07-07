package com.inkpulse.models.response.ghn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GhnWardResponse {
    private String wardCode;
    private Integer districtId;
    private String wardName;
}
