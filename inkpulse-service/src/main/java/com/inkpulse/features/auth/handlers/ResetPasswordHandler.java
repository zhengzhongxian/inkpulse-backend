package com.inkpulse.features.auth.handlers;

import com.inkpulse.cache.SectionCacheService;
import com.inkpulse.constants.message.AuthMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.entities.User;
import com.inkpulse.features.auth.commands.ResetPasswordCommand;
import com.inkpulse.features.auth.dto.ForgotPasswordCooldownDto;
import com.inkpulse.features.auth.dto.ForgotPasswordSessionDto;
import com.inkpulse.repositories.UserRepository;
import com.inkpulse.cqrs.Command;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResetPasswordHandler implements Command.CommandHandler<ResetPasswordCommand, Void> {

    private final UserRepository userRepository;
    private final SectionCacheService sectionCache;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Void handle(ResetPasswordCommand cmd) {
        // 1. Check confirm password match
        if (!cmd.getNewPassword().equals(cmd.getConfirmPassword())) {
            throw new BusinessValidationException(AuthMessageConstants.PASSWORD_CONFIRM_MISMATCH, "PASSWORD_MISMATCH");
        }

        // 2. Fetch session from Cache
        ForgotPasswordSessionDto session = sectionCache.get(cmd.getToken(), ForgotPasswordSessionDto.class);
        if (session == null) {
            throw new BusinessValidationException(AuthMessageConstants.INVALID_RESET_TOKEN, "INVALID_TOKEN");
        }

        // 3. Resolve User
        User user = userRepository.findById(session.userId())
                .orElseThrow(() -> new BusinessValidationException(com.inkpulse.constants.message.UserMessageConstants.USER_NOT_FOUND, "USER_NOT_FOUND"));

        // 4. Update Password
        user.setPassword(passwordEncoder.encode(cmd.getNewPassword()));
        user.setPasswordChangeAt(LocalDateTime.now());
        userRepository.save(user);

        // 5. Clean up Cache
        sectionCache.remove(cmd.getToken(), ForgotPasswordSessionDto.class);
        sectionCache.remove("cooldown:" + session.email(), ForgotPasswordCooldownDto.class);

        log.info("Reset password successful for user: {}", user.getUsername());
        return null;
    }
}
