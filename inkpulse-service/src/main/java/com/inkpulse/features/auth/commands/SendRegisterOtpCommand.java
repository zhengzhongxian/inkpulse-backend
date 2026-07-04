package com.inkpulse.features.auth.commands;

import com.inkpulse.cqrs.Command;
import lombok.Getter;

@Getter
public class SendRegisterOtpCommand implements Command<Void> {
    private final String email;
    private final String deviceId;
    private final String browserFingerprint;

    public SendRegisterOtpCommand(String email, String deviceId, String browserFingerprint) {
        this.email = email;
        this.deviceId = deviceId;
        this.browserFingerprint = browserFingerprint;
    }
}
