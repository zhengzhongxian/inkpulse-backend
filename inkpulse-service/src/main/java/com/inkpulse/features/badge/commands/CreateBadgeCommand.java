package com.inkpulse.features.badge.commands;

import com.inkpulse.cqrs.Command;
import com.inkpulse.models.response.badge.BadgeResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBadgeCommand implements Command<BadgeResponse> {
    private String text;
    private String textColor;
    private String bgColor;
    private String shape;
}
