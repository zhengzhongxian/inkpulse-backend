package com.inkpulse.features.auth.commands;

import com.inkpulse.cqrs.Command;
import com.inkpulse.models.response.auth.LoginResult;
import lombok.Getter;

@Getter
public class RefreshTokenCommand implements Command<LoginResult> {

    private final String refreshToken;

    public RefreshTokenCommand(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
