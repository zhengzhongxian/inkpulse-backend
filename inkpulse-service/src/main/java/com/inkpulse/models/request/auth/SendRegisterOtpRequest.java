package com.inkpulse.models.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendRegisterOtpRequest {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Device ID không được để trống")
    private String deviceId;

    @NotBlank(message = "Fingerprint không được để trống")
    private String browserFingerprint;
}
