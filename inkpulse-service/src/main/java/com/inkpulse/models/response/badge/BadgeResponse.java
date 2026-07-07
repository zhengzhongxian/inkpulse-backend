package com.inkpulse.models.response.badge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BadgeResponse {
    private UUID id;
    private String text;
    private String textColor;
    private String bgColor;
    private String shape;
}
