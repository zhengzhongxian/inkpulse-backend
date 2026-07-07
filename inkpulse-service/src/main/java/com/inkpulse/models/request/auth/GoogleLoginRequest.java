package com.inkpulse.models.request.auth;

import com.inkpulse.constants.message.AuthMessageConstants;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleLoginRequest {
    @NotBlank(message = AuthMessageConstants.ID_TOKEN_NOT_BLANK)
    private String idToken;

    @NotBlank(message = AuthMessageConstants.DEVICE_ID_NOT_BLANK)
    private String deviceId;

    @NotBlank(message = AuthMessageConstants.FINGERPRINT_NOT_BLANK)
    private String browserFingerprint;

    @NotBlank(message = AuthMessageConstants.DEVICE_NAME_NOT_BLANK)
    private String deviceName;

    @NotBlank(message = AuthMessageConstants.DEVICE_TYPE_NOT_BLANK)
    private String deviceType;
}
