package com.inkpulse.features.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleUserPayload {
    private String googleUserId;
    private String email;
    private boolean emailVerified;
    private String name;
    private String givenName;
    private String familyName;
    private String picture;
}
