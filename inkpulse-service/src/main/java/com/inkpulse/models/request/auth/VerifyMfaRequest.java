package com.inkpulse.models.request.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyMfaRequest {
    private String mfaSessionId;
    private String code;
    private String deviceId;
    private String browserFingerprint;
}
