package com.inkpulse.features.auth.commands;

import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.User;
import com.inkpulse.models.response.auth.LoginResult;
import lombok.Getter;
import lombok.Setter;

@Getter
public class LoginCommand implements Command<LoginResult> {

    private final String login;
    private final String password;
    private final String deviceId;
    private final String browserFingerprint;
    private final String deviceName;
    private final String deviceType;
    private final String clientIp;

    @Setter
    private User resolvedUser;

    @Setter
    private boolean passwordCorrect;

    public LoginCommand(String login, String password, String deviceId,
                         String browserFingerprint, String deviceName,
                         String deviceType, String clientIp) {
        this.login = login;
        this.password = password;
        this.deviceId = deviceId;
        this.browserFingerprint = browserFingerprint;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.clientIp = clientIp;
    }
}
