package com.inkpulse.features.auth.rules;

import com.inkpulse.constants.message.AuthMessageConstants;
import com.inkpulse.entities.User;
import com.inkpulse.entities.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.inkpulse.pipeline.EligibilityContext;
import com.inkpulse.pipeline.IEligibilityRule;

@Slf4j
@Component
@RequiredArgsConstructor
public class CheckUserStatusRule implements IEligibilityRule<LoginPipelineContext> {

    @Override
    public int getOrder() {
        return 3;
    }

    @Override
    public void evaluate(EligibilityContext<LoginPipelineContext> context) {
        LoginPipelineContext loginContext = context.getEntity();
        User user = loginContext.getEntity().getResolvedUser();

        if (user == null) {
            context.reject(AuthMessageConstants.LOGIN_INVALID_CREDENTIALS);
            return;
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            context.reject(AuthMessageConstants.LOGIN_ACCOUNT_DISABLED);
            log.warn("Login attempt for non-active account: {}", user.getUsername());
            return;
        }

        if (!user.isVerified()) {
            context.reject(AuthMessageConstants.LOGIN_ACCOUNT_NOT_VERIFIED);
            log.warn("Login attempt for unverified account: {}", user.getUsername());
            return;
        }
    }
}
