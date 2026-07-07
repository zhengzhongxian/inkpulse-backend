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
public class ResetPasswordCommand implements Command<Void> {
    private String token;
    private String newPassword;
    private String confirmPassword;
}
