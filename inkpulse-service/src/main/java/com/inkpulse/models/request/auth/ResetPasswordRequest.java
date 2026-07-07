package com.inkpulse.models.request.auth;

import com.inkpulse.constants.message.AuthMessageConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {
    @NotBlank(message = AuthMessageConstants.TOKEN_NOT_BLANK)
    private String token;

    @NotBlank(message = AuthMessageConstants.PASSWORD_NEW_NOT_BLANK)
    @Size(min = 6, message = AuthMessageConstants.PASSWORD_MIN_SIZE)
    private String newPassword;

    @NotBlank(message = AuthMessageConstants.PASSWORD_CONFIRM_NOT_BLANK)
    private String confirmPassword;
}
