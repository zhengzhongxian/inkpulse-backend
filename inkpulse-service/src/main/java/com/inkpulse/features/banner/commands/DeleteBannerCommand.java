package com.inkpulse.features.banner.commands;

import com.inkpulse.cqrs.Command;
import lombok.Value;

import java.util.UUID;

@Value
public class DeleteBannerCommand implements Command<Void> {
    UUID bannerId;
}
