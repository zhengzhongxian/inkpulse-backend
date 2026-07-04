package com.inkpulse.features.auth.handlers;

import com.inkpulse.cqrs.Command;
import com.inkpulse.constants.message.MfaMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.entities.*;
import com.inkpulse.entities.enums.DisplayMode;
import com.inkpulse.entities.enums.Language;
import com.inkpulse.features.auth.service.MfaService;
import com.inkpulse.features.auth.service.TokenService;
import com.inkpulse.features.auth.commands.VerifyMfaCommand;
import com.inkpulse.models.response.LoginResult;
import com.inkpulse.features.auth.dto.MfaVerificationSessionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.inkpulse.repositories.*;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class VerifyMfaHandler implements Command.CommandHandler<VerifyMfaCommand, LoginResult> {

    private final MfaService mfaService;
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final UserSettingRepository userSettingRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserPermissionRepository userPermissionRepository;

    @Override
    @Transactional
    public LoginResult handle(VerifyMfaCommand cmd) {
        MfaVerificationSessionDto session = mfaService.getSession(cmd.getMfaSessionId());
        if (session == null) {
            throw new BusinessValidationException(MfaMessageConstants.INVALID_SESSION, "MFA_SESSION_NOT_FOUND");
        }

        boolean verified = "APPROVED".equals(session.mfaType()) || mfaService.verifyOtp(cmd.getMfaSessionId(), cmd.getCode());
        if (!verified) {
            throw new BusinessValidationException(MfaMessageConstants.INVALID_CODE, "MFA_INVALID_CODE");
        }

        UUID userId = session.userId();
        UUID deviceId = session.deviceId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessValidationException("Không tìm thấy người dùng", "USER_NOT_FOUND"));
        UserDevice device = userDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new BusinessValidationException("Không tìm thấy thiết bị", "DEVICE_NOT_FOUND"));

        mfaService.invalidateSessionAndLimits(cmd.getMfaSessionId(), user.getEmail());

        List<String> roles = collectRoles(userId);
        List<String> permissions = collectPermissions(userId);
        UserSetting setting = userSettingRepository.findByUserId(userId).orElse(null);

        tokenService.storeUserSession(
                userId, roles, permissions,
                setting != null ? setting.getDisplayMode() : DisplayMode.SYSTEM,
                setting != null ? setting.getChoiceLanguage() : Language.VI
        );

        // Eager cache cart items
        tokenService.cacheUserCart(userId);

        String accessToken = tokenService.generateAccessToken(userId, user.getUsername(), roles);
        String refreshToken = tokenService.generateRefreshToken(userId, deviceId);

        user.setLastLoginAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        device.setLastLoginAt(java.time.LocalDateTime.now());
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
