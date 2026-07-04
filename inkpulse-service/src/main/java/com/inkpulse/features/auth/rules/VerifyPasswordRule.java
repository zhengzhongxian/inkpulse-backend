package com.inkpulse.features.auth.rules;

import com.inkpulse.constants.message.AuthMessageConstants;
import com.inkpulse.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import com.inkpulse.pipeline.EligibilityContext;
import com.inkpulse.pipeline.IEligibilityRule;

@Slf4j
@Component
@RequiredArgsConstructor
public class VerifyPasswordRule implements IEligibilityRule<LoginPipelineContext> {

    private final PasswordEncoder passwordEncoder;

    @Override
    public int getOrder() {
        return 4;
    }

    @Override
    public void evaluate(EligibilityContext<LoginPipelineContext> context) {
        LoginPipelineContext loginContext = context.getEntity();
        User user = loginContext.getEntity().getResolvedUser();

        if (user == null || context.isRejected()) return;

        boolean matches = passwordEncoder.matches(loginContext.getEntity().getPassword(), user.getPassword());
        loginContext.getEntity().setPasswordCorrect(matches);

        if (!matches) {
            context.warn(AuthMessageConstants.LOGIN_INVALID_CREDENTIALS);
        }
    }
}
