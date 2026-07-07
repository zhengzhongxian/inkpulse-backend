package com.inkpulse.features.auth.commands;

import com.inkpulse.cqrs.Command;
import com.inkpulse.models.response.auth.LoginResult;
import lombok.Getter;

@Getter
public class VerifyMfaCommand implements Command<LoginResult> {

    private final String mfaSessionId;
    private final String code;
    private final String deviceId;
    private final String browserFingerprint;
    private final String clientIp;

    public VerifyMfaCommand(String mfaSessionId, String code, String deviceId,
                             String browserFingerprint, String clientIp) {
        this.mfaSessionId = mfaSessionId;
        this.code = code;
        this.deviceId = deviceId;
        this.browserFingerprint = browserFingerprint;
        this.clientIp = clientIp;
    }
}
