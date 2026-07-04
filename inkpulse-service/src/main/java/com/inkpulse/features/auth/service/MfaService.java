package com.inkpulse.features.auth.service;

import com.inkpulse.cache.SectionCacheService;
import com.github.f4b6a3.uuid.UuidCreator;
import com.inkpulse.constants.message.MfaMessageConstants;
import com.inkpulse.constants.message.OtpMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.entities.MfaConfig;
import com.inkpulse.entities.enums.MfaType;
import com.inkpulse.features.auth.dto.LoginOtpSessionDto;
import com.inkpulse.features.auth.dto.MfaVerificationSessionDto;
import com.inkpulse.features.auth.dto.OtpLimitDeviceDto;
import com.inkpulse.features.auth.dto.OtpLimitEmailDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.inkpulse.constants.QueueConstants;
import com.inkpulse.features.auth.dto.SendNumberChallengeEmailMessage;
import com.inkpulse.service.outbox.OutboxPublisher;
import com.inkpulse.repositories.UserRepository;
import com.inkpulse.entities.User;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MfaService {

    private final SectionCacheService sectionCache;
    private final OutboxPublisher outboxPublisher;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public String createMfaSession(UUID userId, UUID deviceId, List<MfaConfig> mfaConfigs) {
        String sessionId = UuidCreator.getTimeOrderedEpoch().toString();

        List<MfaVerificationSessionDto.MfaConfigItem> configItems;
        if (mfaConfigs == null || mfaConfigs.isEmpty()) {
            configItems = List.of(new MfaVerificationSessionDto.MfaConfigItem(
                    MfaType.EMAIL.name(),
                    "Xác thực qua Email OTP",
                    UUID.randomUUID().toString()
            ));
        } else {
            configItems = mfaConfigs.stream()
                    .map(mc -> new MfaVerificationSessionDto.MfaConfigItem(
                            mc.getMfaType().getTypeName().name(),
                            mc.getMfaType().getDisplayName(),
                            mc.getId().toString()
                    ))
                    .toList();
        }

        MfaVerificationSessionDto session = new MfaVerificationSessionDto(
                sessionId, userId, deviceId,
                0, "PENDING", null, configItems,
                null, null
        );

        sectionCache.set(session);
        return sessionId;
    }

    /**
     * Number-matching email challenge (giống Google Prompt):
     * 1. Sinh challengeNumber (số đúng hiển thị trên login screen)
     * 2. Sinh 2 decoy ngẫu nhiên khác challengeNumber
     * 3. Shuffle 3 số
     * 4. Gửi email với 3 lựa chọn dạng button
     *
     * @return challengeNumber — backend trả về cho frontend hiển thị trên màn hình login
     */
    @Transactional
    public int sendEmailNumberChallenge(String sessionId, String clientProvidedEmail) {
        MfaVerificationSessionDto session = validateSession(sessionId);
        User user = userRepository.findById(session.userId())
                .orElseThrow(() -> new BusinessValidationException("Không tìm thấy người dùng", "USER_NOT_FOUND"));
        String targetEmail = user.getEmail();

        String deviceIdStr = session.deviceId().toString();
        checkOtpLimits(targetEmail, deviceIdStr);

        int challengeNumber = generateTwoDigitNumber();
        List<Integer> options = buildChallengeOptions(challengeNumber);

        // Store challenge in session: verificationValue = challengeNumber string
        updateSessionNumberChallenge(sessionId, challengeNumber, options);

        // Store challenge in separate key for Replay Attack prevention using SectionCache
        sectionCache.set(new LoginOtpSessionDto(targetEmail, String.valueOf(challengeNumber)));

        SendNumberChallengeEmailMessage emailMsg = SendNumberChallengeEmailMessage.builder()
                .email(targetEmail)
                .subject("InkPulse - Xác thực đăng nhập")
                .challengeNumber(challengeNumber)
                .options(options)
                .sessionId(sessionId)
                .build();
        outboxPublisher.publish(
                QueueConstants.SEND_CHALLENGE_EMAIL,
                emailMsg,
                "urn:message:InkPulse.Worker.Features.Auth.Messages:SendNumberChallengeEmailMessage"
        );

        incrementOtpLimit(targetEmail, deviceIdStr);
        log.info("Number challenge sent to email {}; challenge={}", targetEmail, challengeNumber);
        return challengeNumber;
    }

    public String initiatePushMfa(String sessionId) {
        MfaVerificationSessionDto session = validateSession(sessionId);
        int challenge = generateTwoDigitNumber();
        String challengeStr = String.valueOf(challenge);
        updateSessionOtp(sessionId, "PUSH", challengeStr);
        return challengeStr;
    }

    public void approveMfaSession(String sessionId) {
        approveSession(sessionId);
    }

    public boolean verifyOtp(String sessionId, String submittedCode) {
        MfaVerificationSessionDto session = validateSession(sessionId);
        User user = userRepository.findById(session.userId())
                .orElseThrow(() -> new BusinessValidationException("Không tìm thấy người dùng", "USER_NOT_FOUND"));
        String targetEmail = user.getEmail();

        LoginOtpSessionDto otpSession = sectionCache.get(targetEmail, LoginOtpSessionDto.class);
        if (otpSession == null) {
            throw new BusinessValidationException(MfaMessageConstants.INVALID_SESSION, "MFA_NO_OTP");
        }

        if (otpSession.otpCode().equals(submittedCode)) {
            // DEL key immediately upon correct match to prevent Replay Attack
            sectionCache.remove(targetEmail, LoginOtpSessionDto.class);
            approveSession(sessionId);
            return true;
        }

        // If wrong option clicked, immediately invalidate session to notify FE
        invalidateSession(sessionId);
        sectionCache.remove(targetEmail, LoginOtpSessionDto.class);
        return false;
    }

    public MfaVerificationSessionDto getSession(String sessionId) {
        return sectionCache.get(sessionId, MfaVerificationSessionDto.class);
    }

    public boolean isSessionApproved(String sessionId) {
        MfaVerificationSessionDto session = getSession(sessionId);
        return session != null && "APPROVED".equals(session.mfaType());
    }

    public UUID getUserIdFromSession(String sessionId) {
        MfaVerificationSessionDto session = getSession(sessionId);
        return session != null ? session.userId() : null;
    }

    public UUID getDeviceIdFromSession(String sessionId) {
        MfaVerificationSessionDto session = getSession(sessionId);
        return session != null ? session.deviceId() : null;
    }

    public void invalidateSession(String sessionId) {
        sectionCache.remove(sessionId, MfaVerificationSessionDto.class);
    }

    public void invalidateSessionAndLimits(String sessionId, String email) {
        MfaVerificationSessionDto session = getSession(sessionId);
        UUID deviceId = session != null ? session.deviceId() : null;

        sectionCache.remove(sessionId, MfaVerificationSessionDto.class);
        sectionCache.remove(email, OtpLimitEmailDto.class);
        if (deviceId != null) {
            sectionCache.remove(deviceId.toString(), OtpLimitDeviceDto.class);
        }
        sectionCache.remove(email, LoginOtpSessionDto.class);
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private MfaVerificationSessionDto validateSession(String sessionId) {
        MfaVerificationSessionDto session = sectionCache.get(sessionId, MfaVerificationSessionDto.class);
        if (session == null) {
            throw new BusinessValidationException(MfaMessageConstants.INVALID_SESSION, "MFA_SESSION_NOT_FOUND");
        }
        return session;
    }

    /** Generates a random 2-digit number (10–99) */
    private int generateTwoDigitNumber() {
        return 10 + secureRandom.nextInt(90);
    }

    /**
     * Builds list of 3 shuffled options: 1 correct + 2 distinct decoys.
     * Decoys are guaranteed to be different from challengeNumber and each other.
     */
    private List<Integer> buildChallengeOptions(int challengeNumber) {
        List<Integer> options = new ArrayList<>();
        options.add(challengeNumber);
        while (options.size() < 3) {
            int decoy = generateTwoDigitNumber();
            if (!options.contains(decoy)) {
                options.add(decoy);
            }
        }
        Collections.shuffle(options, secureRandom);
        return options;
    }

    private void updateSessionNumberChallenge(String sessionId, int challengeNumber, List<Integer> options) {
        MfaVerificationSessionDto session = validateSession(sessionId);
        MfaVerificationSessionDto updated = new MfaVerificationSessionDto(
                session.sessionId(),
                session.userId(),
                session.deviceId(),
                session.attemptCount(),
                MfaType.EMAIL.name(),
                String.valueOf(challengeNumber),  // verificationValue = correct answer
                session.mfaConfigs(),
                challengeNumber,
                options
        );
        sectionCache.set(updated);
    }

    private void updateSessionOtp(String sessionId, String mfaType, String otp) {
        MfaVerificationSessionDto session = validateSession(sessionId);
        MfaVerificationSessionDto updated = new MfaVerificationSessionDto(
                session.sessionId(),
                session.userId(),
                session.deviceId(),
                session.attemptCount(),
                mfaType,
                otp,
                session.mfaConfigs(),
                session.challengeNumber(),
                session.challengeOptions()
        );
        sectionCache.set(updated);
    }

    private void approveSession(String sessionId) {
        MfaVerificationSessionDto session = validateSession(sessionId);
        MfaVerificationSessionDto updated = new MfaVerificationSessionDto(
                session.sessionId(),
                session.userId(),
                session.deviceId(),
                session.attemptCount(),
                "APPROVED",
                session.verificationValue(),
                session.mfaConfigs(),
                session.challengeNumber(),
                session.challengeOptions()
        );
        sectionCache.set(updated);
    }

    private boolean incrementAttemptAndMaybeInvalidate(String sessionId, MfaVerificationSessionDto session) {
        int attempts = session.attemptCount() + 1;

        if (attempts >= 5) {
            sectionCache.remove(sessionId, MfaVerificationSessionDto.class);
            throw new BusinessValidationException(MfaMessageConstants.MAX_ATTEMPTS, "MFA_MAX_ATTEMPTS");
        }

        MfaVerificationSessionDto updated = new MfaVerificationSessionDto(
                session.sessionId(),
                session.userId(),
                session.deviceId(),
                attempts,
                session.mfaType(),
                session.verificationValue(),
                session.mfaConfigs(),
                session.challengeNumber(),
                session.challengeOptions()
        );
        sectionCache.set(updated);

        return false;
    }

    private void checkOtpLimits(String email, String sessionId) {
        OtpLimitEmailDto emailLimit = sectionCache.get(email, OtpLimitEmailDto.class);
        if (emailLimit != null && emailLimit.requestCount() >= 5) {
            throw new BusinessValidationException(OtpMessageConstants.EMAIL_DAILY_LIMITED, "OTP_EMAIL_LIMIT");
        }

        OtpLimitDeviceDto deviceLimit = sectionCache.get(sessionId, OtpLimitDeviceDto.class);
        if (deviceLimit != null && deviceLimit.requestCount() >= 5) {
            throw new BusinessValidationException(OtpMessageConstants.DEVICE_HOURLY_LIMITED, "OTP_DEVICE_LIMIT");
        }
    }

    private void incrementOtpLimit(String email, String sessionId) {
        OtpLimitEmailDto emailLimit = sectionCache.get(email, OtpLimitEmailDto.class);
        int emailCount = emailLimit == null ? 0 : emailLimit.requestCount();
        sectionCache.set(new OtpLimitEmailDto(email, emailCount + 1));

        OtpLimitDeviceDto deviceLimit = sectionCache.get(sessionId, OtpLimitDeviceDto.class);
        int deviceCount = deviceLimit == null ? 0 : deviceLimit.requestCount();
        sectionCache.set(new OtpLimitDeviceDto(sessionId, deviceCount + 1));
    }
}
