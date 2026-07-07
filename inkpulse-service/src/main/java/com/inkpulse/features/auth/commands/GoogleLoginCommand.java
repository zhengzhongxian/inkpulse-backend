package com.inkpulse.features.auth.commands;

import com.inkpulse.cqrs.Command;
import com.inkpulse.models.response.auth.GoogleLoginResult;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleLoginCommand implements Command<GoogleLoginResult> {
    private String idToken;
    private String deviceId;
    private String browserFingerprint;
    private String deviceName;
    private String deviceType;
    private String clientIp;
}
