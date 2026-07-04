package com.inkpulse.features.auth.handlers;

import com.inkpulse.cache.SectionCacheService;
import com.inkpulse.cqrs.Command;
import com.inkpulse.constants.message.AuthMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.entities.*;
import com.inkpulse.entities.enums.*;
import com.inkpulse.features.auth.commands.LoginCommand;
import com.inkpulse.features.auth.service.MfaService;
import com.inkpulse.features.auth.service.TokenService;
import com.inkpulse.service.outbox.OutboxPublisher;
import com.inkpulse.constants.QueueConstants;
import com.inkpulse.features.auth.dto.SendNewDeviceAlertEmailMessage;
import com.inkpulse.features.auth.dto.AccountLockDto;
import com.inkpulse.features.auth.dto.LoginAttemptsDto;
import com.inkpulse.models.response.LoginResult;
import com.inkpulse.features.auth.rules.LoginPipelineContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.inkpulse.pipeline.EligibilityPipeline;
import com.inkpulse.pipeline.IEligibilityRule;
import com.inkpulse.repositories.*;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginHandler implements Command.CommandHandler<LoginCommand, LoginResult> {

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final UserSettingRepository userSettingRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final MfaConfigRepository mfaConfigRepository;
    private final AccountLockLogRepository accountLockLogRepository;
    private final TokenService tokenService;
    private final MfaService mfaService;
    private final SectionCacheService sectionCache;
    private final OutboxPublisher outboxPublisher;
    private final List<IEligibilityRule<LoginPipelineContext>> loginRules;

    @Override
    @Transactional
    public LoginResult handle(LoginCommand cmd) {
        User user = resolveUser(cmd);
        cmd.setResolvedUser(user);

        LoginAttemptsDto attempts = sectionCache.get(cmd.getLogin(), LoginAttemptsDto.class);
        boolean wasBruteForced = attempts != null && attempts.failCount() >= 5;

        LoginPipelineContext context = new LoginPipelineContext(cmd, cmd.getClientIp());
        if (user != null) {
            context.setUserId(user.getId());
        }

        EligibilityPipeline<LoginPipelineContext> eligibilityPipeline = new EligibilityPipeline<>(loginRules);
        var resultContext = eligibilityPipeline.run(context);

        if (resultContext.isRejected()) {
            throw new BusinessValidationException(resultContext.getRejectionReason(), "LOGIN_REJECTED");
        }

        handlePasswordResult(cmd, user);

        if (!cmd.isPasswordCorrect()) {
            throw new BusinessValidationException(AuthMessageConstants.LOGIN_INVALID_CREDENTIALS, "INVALID_CREDENTIALS");
        }

        UUID deviceId = parseDeviceId(cmd.getDeviceId());
        UserDevice device = validateAndSyncDevice(user, deviceId,
                cmd.getBrowserFingerprint(), cmd.getDeviceName(), cmd.getDeviceType(), cmd.getClientIp());

        if (user.isMfaEnabled() || wasBruteForced) {
            return initiateMfaChallenge(user, device.getId());
        }

        return issueTokens(user, device);
    }

    private User resolveUser(LoginCommand cmd) {
        return userRepository.findByUsernameOrEmail(cmd.getLogin(), cmd.getLogin())
                .orElse(null);
    }

    private void handlePasswordResult(LoginCommand cmd, User user) {
        if (user == null) {
            return;
        }
        if (!cmd.isPasswordCorrect()) {
            incrementFailedAttempts(cmd.getLogin(), cmd.getClientIp());
        } else {
            clearFailedAttempts(cmd.getLogin());
        }
    }

    private void incrementFailedAttempts(String login, String ipAddress) {
        LoginAttemptsDto attempts = sectionCache.get(login, LoginAttemptsDto.class);
        int count = attempts == null ? 1 : attempts.failCount() + 1;

        LoginAttemptsDto updated = new LoginAttemptsDto(
                login,
                count,
                ipAddress,
                LocalDateTime.now()
        );
        sectionCache.set(updated);
    }

    private void clearFailedAttempts(String login) {
        sectionCache.remove(login, LoginAttemptsDto.class);
    }


    private UUID parseDeviceId(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            return UUID.randomUUID();
        }
        try {
            return UUID.fromString(deviceId);
        } catch (IllegalArgumentException e) {
            return UUID.randomUUID();
        }
    }

    private UserDevice validateAndSyncDevice(User user, UUID deviceId, String fingerprint,
                                              String deviceName, String deviceType, String ipAddress) {
        Optional<UserDevice> existing = userDeviceRepository.findByUserIdAndId(user.getId(), deviceId);

        if (existing.isPresent()) {
            UserDevice device = existing.get();
            if (fingerprint != null && !fingerprint.equals(device.getBrowserFingerprint())) {
                device.setBrowserFingerprint(fingerprint);
            }
            device.setLastLoginAt(LocalDateTime.now());
            return userDeviceRepository.save(device);
        }

        DeviceType type = DeviceType.DESKTOP;
        try {
            if (deviceType != null) {
                type = DeviceType.valueOf(deviceType.toUpperCase());
            }
        } catch (IllegalArgumentException ignored) {
        }

        UserDevice newDevice = new UserDevice();
        newDevice.setUser(user);
        newDevice.setDeviceName(deviceName != null ? deviceName : "Unknown Device");
        newDevice.setDeviceType(type);
        newDevice.setBrowserFingerprint(fingerprint);
        newDevice.setTrusted(false);
        newDevice.setLastLoginAt(LocalDateTime.now());
        newDevice = userDeviceRepository.save(newDevice);

        SendNewDeviceAlertEmailMessage emailMsg = SendNewDeviceAlertEmailMessage.builder()
                .email(user.getEmail())
                .deviceName(newDevice.getDeviceName())
                .ipAddress(ipAddress)
                .build();
        outboxPublisher.publish(
                QueueConstants.SEND_DEVICE_ALERT_EMAIL,
                emailMsg,
                "urn:message:InkPulse.Worker.Features.Auth.Messages:SendNewDeviceAlertEmailMessage"
        );

        return newDevice;
    }

    private LoginResult initiateMfaChallenge(User user, UUID deviceId) {
        List<MfaConfig> mfaConfigs = mfaConfigRepository.findAllByUserId(user.getId());
        List<LoginResult.MfaMethodResponse> methods;
        if (mfaConfigs.isEmpty()) {
            methods = List.of(LoginResult.MfaMethodResponse.builder()
                    .type(MfaType.EMAIL.name())
                    .displayName("Xác thực qua Email OTP")
                    .build());
        } else {
            methods = mfaConfigs.stream()
                    .map(mc -> LoginResult.MfaMethodResponse.builder()
                            .type(mc.getMfaType().getTypeName().name())
                            .displayName(mc.getMfaType().getDisplayName())
                            .build())
                    .toList();
        }

        String sessionId = mfaService.createMfaSession(user.getId(), deviceId, mfaConfigs);
        String maskedEmail = maskEmail(user.getEmail());

        return LoginResult.builder()
                .mfaRequired(true)
                .mfaSessionId(sessionId)
                .supportedMethods(methods)
                .maskedEmail(maskedEmail)
                .build();
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf("@");
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (local.length() <= 2) {
            return local.charAt(0) + "***" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }

    private LoginResult issueTokens(User user, UserDevice device) {
        List<String> roles = collectRoles(user.getId());
        List<String> permissions = collectPermissions(user.getId());
        UserSetting setting = userSettingRepository.findByUserId(user.getId()).orElse(null);

        tokenService.storeUserSession(
                user.getId(), roles, permissions,
                setting != null ? setting.getDisplayMode() : DisplayMode.SYSTEM,
                setting != null ? setting.getChoiceLanguage() : Language.VI
        );

        // Eager cache cart items
        tokenService.cacheUserCart(user.getId());

        String accessToken = tokenService.generateAccessToken(user.getId(), user.getUsername(), roles);
        String refreshToken = tokenService.generateRefreshToken(user.getId(), device.getId());

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        device.setLastLoginAt(LocalDateTime.now());
        userDeviceRepository.save(device);

        return LoginResult.builder()
                .mfaRequired(false)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    private List<String> collectRoles(UUID userId) {
        List<UserRole> userRoles = userRoleRepository.findAllByUserId(userId);
        return userRoles.stream()
                .map(ur -> ur.getRole().getRoleCode())
                .distinct()
                .toList();
    }

    private List<String> collectPermissions(UUID userId) {
        Set<String> permissions = new LinkedHashSet<>();

        List<UserRole> userRoles = userRoleRepository.findAllByUserId(userId);
        for (UserRole ur : userRoles) {
            List<RolePermission> rolePerms = rolePermissionRepository.findAllByRoleId(ur.getRole().getId());
            rolePerms.stream()
                    .map(rp -> rp.getPermission().getPermissionCode())
                    .forEach(permissions::add);
        }

        List<UserPermission> directPerms = userPermissionRepository.findAllByUserId(userId);
        directPerms.stream()
                .map(up -> up.getPermission().getPermissionCode())
                .forEach(permissions::add);

        return new ArrayList<>(permissions);
    }
}
