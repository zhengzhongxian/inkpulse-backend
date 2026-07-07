package com.inkpulse.features.auth.handlers;

import com.inkpulse.cache.SectionCacheService;
import com.inkpulse.constants.QueueConstants;
import com.inkpulse.constants.message.AuthMessageConstants;
import com.inkpulse.constants.message.OtpMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.entities.User;
import com.inkpulse.features.auth.commands.ForgotPasswordCommand;
import com.inkpulse.features.auth.dto.*;
import com.inkpulse.repositories.UserRepository;
import com.inkpulse.cqrs.Command;
import com.inkpulse.service.outbox.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ForgotPasswordHandler implements Command.CommandHandler<ForgotPasswordCommand, Void> {

    private final UserRepository userRepository;
    private final SectionCacheService sectionCache;
    private final OutboxPublisher outboxPublisher;

    @Value("${auth.forgot-password.reset-base-url:http://localhost:5173/reset-password}")
    private String resetBaseUrl;

    @Override
    @Transactional
    public Void handle(ForgotPasswordCommand cmd) {
        String email = cmd.getEmail();
        String deviceId = cmd.getDeviceId();

        // 1. Resolve User
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessValidationException(AuthMessageConstants.EMAIL_NOT_FOUND, "EMAIL_NOT_FOUND"));

        // 2. Check blocks
        OtpBlockEmailDto emailBlock = sectionCache.get(email, OtpBlockEmailDto.class);
        if (emailBlock != null && emailBlock.blocked()) {
            throw new BusinessValidationException(OtpMessageConstants.EMAIL_BLOCKED, "OTP_BLOCKED");
        }

        if (deviceId != null && !deviceId.isBlank()) {
            OtpBlockDeviceDto deviceBlock = sectionCache.get(deviceId, OtpBlockDeviceDto.class);
            if (deviceBlock != null && deviceBlock.blocked()) {
                throw new BusinessValidationException(OtpMessageConstants.DEVICE_BLOCKED, "OTP_BLOCKED");
            }
        }

        // 3. Check Cooldown
        ForgotPasswordCooldownDto cooldown = sectionCache.get("cooldown:" + email, ForgotPasswordCooldownDto.class);
        if (cooldown != null && LocalDateTime.now().isBefore(cooldown.nextRetryAt())) {
            throw new BusinessValidationException(OtpMessageConstants.COOLDOWN_ACTIVE, "OTP_COOLDOWN");
        }

        // 4. Check Email Limit (10 per day - Option B)
        OtpLimitEmailDto emailLimit = sectionCache.get(email, OtpLimitEmailDto.class);
        if (emailLimit != null && emailLimit.requestCount() >= 10) {
            sectionCache.set(new OtpBlockEmailDto(email, true));
            throw new BusinessValidationException(OtpMessageConstants.EMAIL_LIMITED, "OTP_EMAIL_LIMIT");
        }

        // 5. Check Device Limit (5 per hour)
        OtpLimitDeviceDto deviceLimit = null;
        if (deviceId != null && !deviceId.isBlank()) {
            deviceLimit = sectionCache.get(deviceId, OtpLimitDeviceDto.class);
            if (deviceLimit != null && deviceLimit.requestCount() >= 5) {
                sectionCache.set(new OtpBlockDeviceDto(deviceId, true));
                throw new BusinessValidationException(OtpMessageConstants.DEVICE_LIMITED, "OTP_DEVICE_LIMIT");
            }
        }

        // 6. Generate Reset Token
        String token = UUID.randomUUID().toString();
        ForgotPasswordSessionDto session = new ForgotPasswordSessionDto(
                token,
                user.getId(),
                email,
                LocalDateTime.now().plusSeconds(60)
        );
        sectionCache.set(session);

        // 7. Set Cooldown state
        ForgotPasswordCooldownDto newCooldown = new ForgotPasswordCooldownDto(
                email,
                LocalDateTime.now().plusSeconds(60)
        );
        sectionCache.set(newCooldown);

        // 8. Increment Limits
        int emailCount = emailLimit == null ? 0 : emailLimit.requestCount();
        sectionCache.set(new OtpLimitEmailDto(email, emailCount + 1));

        if (deviceId != null && !deviceId.isBlank()) {
            int devCount = deviceLimit == null ? 0 : deviceLimit.requestCount();
            sectionCache.set(new OtpLimitDeviceDto(deviceId, devCount + 1));
        }

        // 9. Publish outbox message for C# Worker
        String name = (user.getProfile() != null && user.getProfile().getFullName() != null)
                ? user.getProfile().getFullName()
                : user.getUsername();

        String resetLink = resetBaseUrl + "?token=" + token;

        SendForgotPasswordEmailMessage emailMsg = SendForgotPasswordEmailMessage.builder()
                .email(email)
                .subject("InkPulse - Yêu cầu đặt lại mật khẩu")
                .name(name)
                .resetLink(resetLink)
                .expiryMinutes(15)
                .build();

        outboxPublisher.publish(
                QueueConstants.SEND_FORGOT_PASSWORD_EMAIL,
                emailMsg,
                "urn:message:InkPulse.Worker.Features.Auth.Messages:SendForgotPasswordEmailMessage"
        );

        log.info("ForgotPassword reset link published to outbox for email: {}, token: {}", email, token);
        return null;
    }
}
