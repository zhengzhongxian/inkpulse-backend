package com.inkpulse.features.badge.commands;

import com.inkpulse.cqrs.Command;
import com.inkpulse.models.response.badge.BadgeResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBadgeCommand implements Command<BadgeResponse> {
    private UUID id;
    private String text;
    private String textColor;
    private String bgColor;
    private String shape;
}
