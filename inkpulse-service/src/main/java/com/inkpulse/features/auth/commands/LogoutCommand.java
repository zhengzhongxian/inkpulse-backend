package com.inkpulse.features.auth.commands;

import com.inkpulse.cqrs.Command;
import lombok.Getter;

@Getter
public class LogoutCommand implements Command<Void> {

    private final String refreshToken;
    private final String accessToken;
    private final String reasonCode;

    public LogoutCommand(String refreshToken, String accessToken, String reasonCode) {
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
        this.reasonCode = reasonCode;
    }
}
