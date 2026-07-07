package com.inkpulse.models.response.auth;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class LoginResult {

    boolean mfaRequired;
    String mfaSessionId;
    List<MfaMethodResponse> supportedMethods;
    String accessToken;
    String refreshToken;
    long expiresIn;
    String maskedEmail;

    @Value
    @Builder
    public static class MfaMethodResponse {
        String type;
        String displayName;
    }
}
