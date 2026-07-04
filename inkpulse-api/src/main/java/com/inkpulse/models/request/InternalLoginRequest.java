package com.inkpulse.models.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InternalLoginRequest {
    @NotBlank(message = "Tên đăng nhập không được để trống")
    private String login;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;
}
