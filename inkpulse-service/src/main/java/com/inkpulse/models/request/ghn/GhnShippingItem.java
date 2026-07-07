package com.inkpulse.models.request.ghn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GhnShippingItem {
    private String name;
    private int quantity;
    private int weight;
    private int length;
    private int width;
    private int height;
}
