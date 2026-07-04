package com.inkpulse.models.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LogoutRequest {

    private String refreshToken;

    /**
     * Reason for logout. Supported values:
     *   DIRECTLY_LOGOUT   — user clicked logout (default)
     *   CHANGE_PASSWORD   — triggered after password change
     *   SECURITY_REVOKE   — admin revoked session
     */
    private String reasonCode;
}
