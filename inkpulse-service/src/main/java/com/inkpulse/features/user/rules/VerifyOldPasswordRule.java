package com.inkpulse.features.user.rules;

import com.inkpulse.constants.message.UserMessageConstants;
import com.inkpulse.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import com.inkpulse.pipeline.EligibilityContext;
import com.inkpulse.pipeline.IEligibilityRule;

@Component
@RequiredArgsConstructor
public class VerifyOldPasswordRule implements IEligibilityRule<ChangePasswordPipelineContext> {

    private final PasswordEncoder passwordEncoder;

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public void evaluate(EligibilityContext<ChangePasswordPipelineContext> context) {
        ChangePasswordPipelineContext passContext = context.getEntity();
        User user = passContext.getUser();

        if (user == null || context.isRejected()) return;

        if (!passwordEncoder.matches(passContext.getEntity().getOldPassword(), user.getPassword())) {
            context.reject(UserMessageConstants.OLD_PASSWORD_INCORRECT);
        }
    }
}
