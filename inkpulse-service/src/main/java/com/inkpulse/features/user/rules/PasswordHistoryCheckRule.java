package com.inkpulse.features.user.rules;

import com.inkpulse.constants.message.UserMessageConstants;
import com.inkpulse.entities.PasswordHistory;
import com.inkpulse.repositories.PasswordHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import com.inkpulse.pipeline.EligibilityContext;
import com.inkpulse.pipeline.IEligibilityRule;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PasswordHistoryCheckRule implements IEligibilityRule<ChangePasswordPipelineContext> {

    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public int getOrder() {
        return 3;
    }

    @Override
    public void evaluate(EligibilityContext<ChangePasswordPipelineContext> context) {
        ChangePasswordPipelineContext passContext = context.getEntity();
        if (context.isRejected()) return;

        List<PasswordHistory> histories = passwordHistoryRepository.findTop5ByUserIdOrderByChangedAtDesc(passContext.getEntity().getUserId());
        for (PasswordHistory history : histories) {
            if (passwordEncoder.matches(passContext.getEntity().getNewPassword(), history.getPassword())) {
                context.reject(UserMessageConstants.PASSWORD_IN_HISTORY);
                return;
            }
        }
    }
}
