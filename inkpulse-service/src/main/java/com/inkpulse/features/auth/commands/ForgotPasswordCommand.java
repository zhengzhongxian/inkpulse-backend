package com.inkpulse.features.auth.commands;

import com.inkpulse.cqrs.Command;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordCommand implements Command<Void> {
    private String email;
    private String deviceId;
    private String browserFingerprint;
}
