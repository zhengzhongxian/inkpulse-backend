package com.inkpulse.features.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendOtpEmailMessage {
    private String email;
    private String subject;
    private String name;
    private String otp;
    private int expiryMinutes;
}
