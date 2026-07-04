package com.inkpulse.features.user.rules;

import com.inkpulse.features.user.commands.ChangePasswordCommand;
import com.inkpulse.entities.User;
import lombok.Getter;
import lombok.Setter;
import com.inkpulse.pipeline.EligibilityContext;

@Getter
@Setter
public class ChangePasswordPipelineContext extends EligibilityContext<ChangePasswordCommand> {
    private User user;

    public ChangePasswordPipelineContext(ChangePasswordCommand command) {
        super(command);
    }
}
