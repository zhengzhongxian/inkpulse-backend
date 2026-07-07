package com.inkpulse.features.auth.handlers;

import com.inkpulse.constants.PermissionConstants;
import com.inkpulse.constants.message.AuthMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.*;
import com.inkpulse.entities.enums.DeviceType;
import com.inkpulse.entities.enums.DisplayMode;
import com.inkpulse.entities.enums.Language;
import com.inkpulse.features.auth.commands.InternalLoginCommand;
import com.inkpulse.features.auth.service.TokenService;
import com.inkpulse.models.response.auth.LoginResult;
import com.inkpulse.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class InternalLoginHandler implements Command.CommandHandler<InternalLoginCommand, LoginResult> {

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final UserSettingRepository userSettingRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public LoginResult handle(InternalLoginCommand cmd) {
        log.info("Handling InternalLoginCommand for login: {}", cmd.getLogin());

        User user = userRepository.findByUsernameOrEmail(cmd.getLogin(), cmd.getLogin())
                .orElseThrow(() -> new BusinessValidationException(
                        AuthMessageConstants.LOGIN_INVALID_CREDENTIALS,
                        "INVALID_CREDENTIALS"
                ));

        if (!passwordEncoder.matches(cmd.getPassword(), user.getPassword())) {
            throw new BusinessValidationException(
                    AuthMessageConstants.LOGIN_INVALID_CREDENTIALS,
                    "INVALID_CREDENTIALS"
            );
        }

        if (!user.getStatus().name().equals("ACTIVE")) {
            throw new BusinessValidationException(
                    AuthMessageConstants.LOGIN_ACCOUNT_DISABLED,
                    "ACCOUNT_DISABLED"
            );
        }

        // Verify user has InternalLogin permission
        List<String> permissions = collectPermissions(user.getId());
        if (!permissions.contains(PermissionConstants.Auth.INTERNAL_LOGIN)) {
            throw new BusinessValidationException(
                    AuthMessageConstants.INTERNAL_LOGIN_NO_PERMISSION,
                    AuthMessageConstants.CODE_INTERNAL_LOGIN_NO_PERMISSION
            );
        }

        // Find or create default dashboard device
        UUID deviceId = UUID.randomUUID();
        UserDevice device = new UserDevice();
        device.setUser(user);
        device.setDeviceName("Dashboard Admin Session");
        device.setDeviceType(DeviceType.DESKTOP);
        device.setBrowserFingerprint("Dashboard-Static");
        device.setTrusted(true);
        device.setLastLoginAt(LocalDateTime.now());
        device = userDeviceRepository.save(device);

        // Issue tokens
        List<String> roles = collectRoles(user.getId());
        UserSetting setting = userSettingRepository.findByUserId(user.getId()).orElse(null);

        tokenService.storeUserSession(
                user.getId(), roles, permissions,
                setting != null ? setting.getDisplayMode() : DisplayMode.SYSTEM,
                setting != null ? setting.getChoiceLanguage() : Language.VI
        );

        tokenService.cacheUserCart(user.getId());

        String accessToken = tokenService.generateAccessToken(user.getId(), user.getUsername(), roles);
        String refreshToken = tokenService.generateRefreshToken(user.getId(), device.getId());

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Internal login successful for user: {}", user.getUsername());

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
