package com.inkpulse.features.auth.handlers;

import com.inkpulse.cache.SectionCacheService;
import com.inkpulse.constants.message.OtpMessageConstants;
import com.inkpulse.constants.message.RegisterMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.features.auth.commands.SendRegisterOtpCommand;
import com.inkpulse.features.auth.dto.*;
import com.inkpulse.constants.QueueConstants;
import com.inkpulse.repositories.UserRepository;
import com.inkpulse.cqrs.Command;
import com.inkpulse.service.outbox.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendRegisterOtpHandler implements Command.CommandHandler<SendRegisterOtpCommand, Void> {

    private final UserRepository userRepository;
    private final SectionCacheService sectionCache;
    private final OutboxPublisher outboxPublisher;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public Void handle(SendRegisterOtpCommand cmd) {
        String email = cmd.getEmail();
        String deviceId = cmd.getDeviceId();

        // 1. Check if email is already taken
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BusinessValidationException(RegisterMessageConstants.EMAIL_TAKEN, "EMAIL_TAKEN");
        }

        // 2. Check if Email or Device is blocked
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

        // 3. Check Cooldown (60s)
        RegisterOtpSessionDto currentSession = sectionCache.get(email, RegisterOtpSessionDto.class);
        if (currentSession != null && LocalDateTime.now().isBefore(currentSession.nextRetryAt())) {
            throw new BusinessValidationException(OtpMessageConstants.COOLDOWN_ACTIVE, "OTP_COOLDOWN");
        }

        // 4. Check Limits (Email limit daily: 5, Device limit hourly: 5)
        OtpLimitEmailDto emailLimit = sectionCache.get(email, OtpLimitEmailDto.class);
        if (emailLimit != null && emailLimit.requestCount() >= 5) {
            sectionCache.set(new OtpBlockEmailDto(email, true));
            throw new BusinessValidationException(OtpMessageConstants.EMAIL_LIMITED, "OTP_EMAIL_LIMIT");
        }

        if (deviceId != null && !deviceId.isBlank()) {
            OtpLimitDeviceDto deviceLimit = sectionCache.get(deviceId, OtpLimitDeviceDto.class);
            if (deviceLimit != null && deviceLimit.requestCount() >= 5) {
                sectionCache.set(new OtpBlockDeviceDto(deviceId, true));
                throw new BusinessValidationException(OtpMessageConstants.DEVICE_LIMITED, "OTP_DEVICE_LIMIT");
            }
        }

        // 5. Generate OTP (6 digits)
        String otpCode = String.format("%06d", 100000 + secureRandom.nextInt(900000));

        // 6. Save OTP session in Redis
        RegisterOtpSessionDto newSession = new RegisterOtpSessionDto(
                email,
                otpCode,
                LocalDateTime.now().plusSeconds(60), // 60s resend cooldown
                0
        );
        sectionCache.set(newSession);

        // 7. Send Email
        SendOtpEmailMessage emailMsg = SendOtpEmailMessage.builder()
                .email(email)
                .subject("InkPulse - Xác thực đăng ký")
                .name(email)
                .otp(otpCode)
                .expiryMinutes(15)
                .build();
        outboxPublisher.publish(
                QueueConstants.SEND_OTP_EMAIL,
                emailMsg,
                "urn:message:InkPulse.Worker.Features.Auth.Messages:SendOtpEmailMessage"
        );

        // 8. Increment Limit Counters
        int emailCount = emailLimit == null ? 0 : emailLimit.requestCount();
        sectionCache.set(new OtpLimitEmailDto(email, emailCount + 1));

        if (deviceId != null && !deviceId.isBlank()) {
            OtpLimitDeviceDto deviceLimit = sectionCache.get(deviceId, OtpLimitDeviceDto.class);
            int deviceCount = deviceLimit == null ? 0 : deviceLimit.requestCount();
            sectionCache.set(new OtpLimitDeviceDto(deviceId, deviceCount + 1));
        }

        log.info("Registration OTP sent to email: {}, code: {}", email, otpCode);
        return null;
    }
}
