package com.inkpulse.features.user.handlers;

import com.inkpulse.constants.message.UserMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.PasswordHistory;
import com.inkpulse.entities.User;
import com.inkpulse.features.user.commands.ChangePasswordCommand;
import com.inkpulse.features.user.rules.ChangePasswordPipelineContext;
import com.inkpulse.pipeline.EligibilityPipeline;
import com.inkpulse.pipeline.IEligibilityRule;
import com.inkpulse.repositories.PasswordHistoryRepository;
import com.inkpulse.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChangePasswordCommandHandler implements Command.CommandHandler<ChangePasswordCommand, Void> {

    private final UserRepository userRepository;
    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final List<IEligibilityRule<ChangePasswordPipelineContext>> passwordRules;

    @Override
    @Transactional
    public Void handle(ChangePasswordCommand cmd) {
        User user = userRepository.findById(cmd.getUserId())
                .orElseThrow(() -> new BusinessValidationException(UserMessageConstants.USER_NOT_FOUND, "USER_NOT_FOUND"));

        // 1. Run eligibility pipeline for checking password change eligibility
        ChangePasswordPipelineContext context = new ChangePasswordPipelineContext(cmd);
        context.setUser(user);

        EligibilityPipeline<ChangePasswordPipelineContext> pipeline = new EligibilityPipeline<>(passwordRules);
        var resultContext = pipeline.run(context);

        if (resultContext.isRejected()) {
            throw new BusinessValidationException(resultContext.getRejectionReason(), "PASSWORD_CHANGE_REJECTED");
        }

        // 2. Encrypt and save new password
        String encodedNewPassword = passwordEncoder.encode(cmd.getNewPassword());
        user.setPassword(encodedNewPassword);

        // 3. Save new password entry to history log
        PasswordHistory passwordHistory = PasswordHistory.builder()
                .user(user)
                .password(encodedNewPassword)
                .changedAt(LocalDateTime.now())
                .changedBy(cmd.getUserId())
                .build();
        passwordHistoryRepository.save(passwordHistory);

        log.info("Successfully changed password and saved history for user: {}", user.getUsername());
        return null;
    }
}
