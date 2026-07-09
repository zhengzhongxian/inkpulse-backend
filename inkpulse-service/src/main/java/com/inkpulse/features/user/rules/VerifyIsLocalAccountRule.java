package com.inkpulse.features.user.rules;

import com.inkpulse.constants.message.UserMessageConstants;
import com.inkpulse.entities.User;
import org.springframework.stereotype.Component;
import com.inkpulse.pipeline.EligibilityContext;
import com.inkpulse.pipeline.IEligibilityRule;

@Component
public class VerifyIsLocalAccountRule implements IEligibilityRule<ChangePasswordPipelineContext> {

    @Override
    public int getOrder() {
        return 0; // Run before VerifyOldPasswordRule
    }

    @Override
    public void evaluate(EligibilityContext<ChangePasswordPipelineContext> context) {
        ChangePasswordPipelineContext passContext = context.getEntity();
        User user = passContext.getUser();

        if (user == null || context.isRejected()) return;

        if (user.getPassword() == null) {
            context.reject(UserMessageConstants.CANNOT_CHANGE_SOCIAL_PASSWORD);
        }
    }
}
