package com.inkpulse.features.auth.rules;

import com.inkpulse.features.auth.commands.LoginCommand;
import lombok.Getter;
import lombok.Setter;
import com.inkpulse.pipeline.EligibilityContext;
import java.util.UUID;

@Getter
@Setter
public class LoginPipelineContext extends EligibilityContext<LoginCommand> {

    private UUID userId;
    private String username;
    private String email;
    private String hashedPassword;
    private boolean mfaEnabled;
    private String clientIp;

    public LoginPipelineContext(LoginCommand command, String clientIp) {
        super(command);
        this.clientIp = clientIp;
    }
}
