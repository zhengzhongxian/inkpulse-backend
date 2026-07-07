package com.inkpulse.models.request.auth;

import com.inkpulse.constants.message.AuthMessageConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordRequest {
    @NotBlank(message = AuthMessageConstants.EMAIL_NOT_BLANK)
    @Email(message = AuthMessageConstants.EMAIL_INVALID)
    private String email;

    @NotBlank(message = AuthMessageConstants.DEVICE_ID_NOT_BLANK)
    private String deviceId;

    @NotBlank(message = AuthMessageConstants.FINGERPRINT_NOT_BLANK)
    private String browserFingerprint;
}
