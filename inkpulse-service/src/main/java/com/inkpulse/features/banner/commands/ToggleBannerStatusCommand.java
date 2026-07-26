package com.inkpulse.features.banner.commands;

import com.inkpulse.cqrs.Command;
import com.inkpulse.models.response.banner.BannerResponse;
import lombok.Value;

import java.util.UUID;

@Value
public class ToggleBannerStatusCommand implements Command<BannerResponse> {
    UUID bannerId;
}
