package com.inkpulse.models.request.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateShippingRequest {
    private String note;
    private String requiredNote;
    private Integer weight;
    private Integer length;
    private Integer width;
    private Integer height;
}
