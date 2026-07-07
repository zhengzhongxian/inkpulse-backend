package com.inkpulse.models.response.auth;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GoogleLoginResult {
    boolean isRegistered;
    String googleUserId;
    String email;
    String name;
    String picture;
    String accessToken;
    String refreshToken;
    long expiresIn;
}
