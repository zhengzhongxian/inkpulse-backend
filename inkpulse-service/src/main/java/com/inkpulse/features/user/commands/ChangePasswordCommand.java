package com.inkpulse.features.user.commands;

import com.inkpulse.cqrs.Command;
import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordCommand implements Command<Void> {
    private UUID userId;
    private String oldPassword;
    private String newPassword;
}
