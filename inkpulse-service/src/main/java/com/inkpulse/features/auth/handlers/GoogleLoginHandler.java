package com.inkpulse.features.auth.handlers;

import com.inkpulse.cache.SectionCacheService;
import com.inkpulse.constants.message.AuthMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.entities.*;
import com.inkpulse.entities.enums.DeviceType;
import com.inkpulse.entities.enums.DisplayMode;
import com.inkpulse.entities.enums.Language;
import com.inkpulse.entities.enums.UserStatus;
import com.inkpulse.features.auth.commands.GoogleLoginCommand;
import com.inkpulse.features.auth.dto.GoogleUserPayload;
import com.inkpulse.features.auth.service.GoogleService;
import com.inkpulse.features.auth.service.TokenService;
import com.inkpulse.models.response.auth.GoogleLoginResult;
import com.inkpulse.repositories.*;
import com.inkpulse.cqrs.Command;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleLoginHandler implements Command.CommandHandler<GoogleLoginCommand, GoogleLoginResult> {

    private final GoogleService googleService;
    private final UserRepository userRepository;
    private final UserSocialAccountRepository userSocialAccountRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final UserSettingRepository userSettingRepository;
    private final TokenService tokenService;

    @Override
    @Transactional
    public GoogleLoginResult handle(GoogleLoginCommand cmd) {
        // 1. Validate Google ID Token
        GoogleUserPayload payload;
        try {
            payload = googleService.validateToken(cmd.getIdToken());
        } catch (Exception e) {
            log.error("Google token validation failed", e);
            throw new BusinessValidationException(AuthMessageConstants.INVALID_GOOGLE_TOKEN, "INVALID_GOOGLE_TOKEN");
        }

        // 2. Check if social account link already exists
        Optional<UserSocialAccount> socialOpt = userSocialAccountRepository.findByProviderAndProviderKey("GOOGLE", payload.getGoogleUserId());

        if (socialOpt.isPresent()) {
            User user = socialOpt.get().getUser();
            return loginAndIssueTokens(user, cmd);
        }

        // 3. Check if email exists locally (matching existing local account)
        Optional<User> localUserOpt = userRepository.findByEmail(payload.getEmail());
        if (localUserOpt.isPresent()) {
            User user = localUserOpt.get();

            // Link Google account to this existing local user
            UserSocialAccount social = UserSocialAccount.builder()
                    .user(user)
                    .provider("GOOGLE")
                    .providerKey(payload.getGoogleUserId())
                    .build();
            userSocialAccountRepository.save(social);
            log.info("Linked Google account to existing user: {}", user.getUsername());

            return loginAndIssueTokens(user, cmd);
        }

        // 4. User is logging in for the first time with Google
        log.info("Google login first time for email: {}, subject: {}", payload.getEmail(), payload.getGoogleUserId());
        return GoogleLoginResult.builder()
                .isRegistered(false)
                .googleUserId(payload.getGoogleUserId())
                .email(payload.getEmail())
                .name(payload.getName())
                .picture(payload.getPicture())
                .build();
    }

    private GoogleLoginResult loginAndIssueTokens(User user, GoogleLoginCommand cmd) {
        // Check account status and verification
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessValidationException(AuthMessageConstants.LOGIN_ACCOUNT_DISABLED, "ACCOUNT_DISABLED");
        }
        if (!user.isVerified()) {
            throw new BusinessValidationException(AuthMessageConstants.LOGIN_ACCOUNT_NOT_VERIFIED, "ACCOUNT_NOT_VERIFIED");
        }

        // Register / sync Device
        UUID deviceId = parseDeviceId(cmd.getDeviceId());
        UserDevice device = validateAndSyncDevice(user, deviceId, cmd.getBrowserFingerprint(),
                cmd.getDeviceName(), cmd.getDeviceType());

        // Gather roles & permissions
        List<String> roles = collectRoles(user.getId());
        List<String> permissions = collectPermissions(user.getId());
        UserSetting setting = userSettingRepository.findByUserId(user.getId()).orElse(null);

        // Store session in Redis
        tokenService.storeUserSession(
                user.getId(), roles, permissions,
                setting != null ? setting.getDisplayMode() : DisplayMode.SYSTEM,
                setting != null ? setting.getChoiceLanguage() : Language.VI
        );

        // Eager cache cart items
        tokenService.cacheUserCart(user.getId());

        // Generate JWT Access & Refresh Tokens
        String accessToken = tokenService.generateAccessToken(user.getId(), user.getUsername(), roles);
        String refreshToken = tokenService.generateRefreshToken(user.getId(), device.getId());

        // Save last login timestamps
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        device.setLastLoginAt(LocalDateTime.now());
        userDeviceRepository.save(device);

        return GoogleLoginResult.builder()
                .isRegistered(true)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(900) // 15 mins
                .build();
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
                                              String deviceName, String deviceType) {
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
        } catch (IllegalArgumentException ignored) {}

        UserDevice newDevice = new UserDevice();
        newDevice.setUser(user);
        newDevice.setDeviceName(deviceName != null ? deviceName : "Unknown Device");
        newDevice.setDeviceType(type);
        newDevice.setBrowserFingerprint(fingerprint);
        newDevice.setTrusted(false);
        newDevice.setLastLoginAt(LocalDateTime.now());
        return userDeviceRepository.save(newDevice);
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
            for (RolePermission rp : rolePerms) {
                permissions.add(rp.getPermission().getPermissionCode());
            }
        }
        List<UserPermission> userPerms = userPermissionRepository.findAllByUserId(userId);
        for (UserPermission up : userPerms) {
            permissions.add(up.getPermission().getPermissionCode());
        }
        return new ArrayList<>(permissions);
    }
}
