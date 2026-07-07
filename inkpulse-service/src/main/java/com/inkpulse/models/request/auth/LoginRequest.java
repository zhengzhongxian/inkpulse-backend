package com.inkpulse.models.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = "Tên đăng nhập hoặc email không được để trống")
    private String login;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;

    @NotBlank(message = "Device ID không được để trống")
    private String deviceId;

    @NotBlank(message = "Fingerprint không được để trống")
    private String browserFingerprint;

    @NotBlank(message = "Tên thiết bị không được để trống")
    private String deviceName;

    @NotBlank(message = "Loại thiết bị không được để trống")
    private String deviceType;
}
