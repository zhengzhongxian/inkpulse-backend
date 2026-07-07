package com.inkpulse.features.auth.handlers;

import com.inkpulse.cache.SectionCacheService;
import com.inkpulse.constants.message.OtpMessageConstants;
import com.inkpulse.constants.message.RegisterMessageConstants;
import com.inkpulse.constants.message.AddressMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.entities.*;
import com.inkpulse.entities.enums.*;
import com.inkpulse.features.auth.commands.VerifyRegisterCommand;
import com.inkpulse.features.auth.dto.*;
import com.inkpulse.features.auth.service.TokenService;
import com.inkpulse.models.response.auth.LoginResult;
import com.inkpulse.repositories.*;
import com.inkpulse.cqrs.Command;
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
public class VerifyRegisterHandler implements Command.CommandHandler<VerifyRegisterCommand, LoginResult> {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final CartRepository cartRepository;
    private final SectionCacheService sectionCache;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final GhnProvinceRepository provinceRepository;
    private final GhnDistrictRepository districtRepository;
    private final GhnWardRepository wardRepository;
    private final UserAddressRepository userAddressRepository;
    private final PermissionRepository permissionRepository;
    private final UserPermissionRepository userPermissionRepository;

    @Override
    @Transactional
    public LoginResult handle(VerifyRegisterCommand cmd) {
        String email = cmd.getEmail();
        String otpCode = cmd.getOtpCode();
        String deviceIdStr = cmd.getDeviceId();

        // 1. Check if blocked
        OtpBlockEmailDto emailBlock = sectionCache.get(email, OtpBlockEmailDto.class);
        if (emailBlock != null && emailBlock.blocked()) {
            throw new BusinessValidationException(OtpMessageConstants.EMAIL_BLOCKED, "OTP_BLOCKED");
        }

        if (deviceIdStr != null && !deviceIdStr.isBlank()) {
            OtpBlockDeviceDto deviceBlock = sectionCache.get(deviceIdStr, OtpBlockDeviceDto.class);
            if (deviceBlock != null && deviceBlock.blocked()) {
                throw new BusinessValidationException(OtpMessageConstants.DEVICE_BLOCKED, "OTP_BLOCKED");
            }
        }

        // 2. Retrieve session OTP from Redis
        RegisterOtpSessionDto otpSession = sectionCache.get(email, RegisterOtpSessionDto.class);
        if (otpSession == null) {
            throw new BusinessValidationException(RegisterMessageConstants.OTP_EXPIRED, "OTP_EXPIRED");
        }

        // 3. Verify OTP
        if (!otpSession.otpCode().equals(otpCode)) {
            int newFailCount = otpSession.failCount() + 1;
            if (newFailCount >= 5) {
                // Remove session OTP and set block
                sectionCache.remove(email, RegisterOtpSessionDto.class);
                sectionCache.set(new OtpBlockEmailDto(email, true));
                if (deviceIdStr != null && !deviceIdStr.isBlank()) {
                    sectionCache.set(new OtpBlockDeviceDto(deviceIdStr, true));
                }
                log.warn("Registration OTP blocked for email: {} due to too many failed attempts.", email);
                throw new BusinessValidationException(RegisterMessageConstants.OTP_BLOCKED_ATTEMPTS, "OTP_BLOCKED");
            } else {
                RegisterOtpSessionDto updatedSession = new RegisterOtpSessionDto(
                        email,
                        otpSession.otpCode(),
                        otpSession.nextRetryAt(),
                        newFailCount
                );
                sectionCache.set(updatedSession);
                int attemptsLeft = 5 - newFailCount;
                throw new BusinessValidationException(
                        RegisterMessageConstants.OTP_INVALID + ". Số lần nhập sai còn lại: " + attemptsLeft,
                        "OTP_INVALID"
                );
            }
        }

        // OTP matched - clean it up
        sectionCache.remove(email, RegisterOtpSessionDto.class);
        sectionCache.remove(email, OtpLimitEmailDto.class);
        sectionCache.remove(email, OtpBlockEmailDto.class);
        if (deviceIdStr != null && !deviceIdStr.isBlank()) {
            sectionCache.remove(deviceIdStr, OtpLimitDeviceDto.class);
            sectionCache.remove(deviceIdStr, OtpBlockDeviceDto.class);
        }

        // 4. Validate duplicate usernames and emails
        if (userRepository.findByUsername(cmd.getUserName()).isPresent()) {
            throw new BusinessValidationException(RegisterMessageConstants.USERNAME_TAKEN, "USERNAME_TAKEN");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new BusinessValidationException(RegisterMessageConstants.EMAIL_TAKEN, "EMAIL_TAKEN");
        }

        // 5. Build and save the User (with cascaded profile and settings)
        User user = User.builder()
                .username(cmd.getUserName())
                .password(passwordEncoder.encode(cmd.getPassword()))
                .email(email)
                .status(UserStatus.ACTIVE)
                .verified(true)
                .mfaEnabled(false)
                .lastLoginAt(LocalDateTime.now())
                .build();

        String fullName = (cmd.getFirstName() != null ? cmd.getFirstName() : "") + " " +
                (cmd.getLastName() != null ? cmd.getLastName() : "");
        fullName = fullName.trim();

        Gender gender = Gender.UNKNOWN;
        if (cmd.getGender() != null) {
            try {
                gender = Gender.valueOf(cmd.getGender().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        UserProfile profile = UserProfile.builder()
                .user(user)
                .firstName(cmd.getFirstName())
                .lastName(cmd.getLastName())
                .fullName(fullName.isEmpty() ? null : fullName)
                .gender(gender)
                .dob(cmd.getDob())
                .build();

        Language lang = Language.VI;
        if (cmd.getChoiceLanguage() != null) {
            try {
                lang = Language.valueOf(cmd.getChoiceLanguage().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        UserSetting setting = UserSetting.builder()
                .user(user)
                .displayMode(DisplayMode.LIGHT)
                .choiceLanguage(lang)
                .build();

        user.setProfile(profile);
        user.setSetting(setting);

        // Persist User (along with Profile and Setting via CascadeType.ALL)
        user = userRepository.save(user);

        // Fetch address dependencies, validate hierarchy, and save UserAddress
        GhnProvince province = provinceRepository.findById(cmd.getProvinceId())
                .orElseThrow(() -> new BusinessValidationException(AddressMessageConstants.PROVINCE_NOT_FOUND, "PROVINCE_NOT_FOUND"));
        GhnDistrict district = districtRepository.findById(cmd.getDistrictId())
                .orElseThrow(() -> new BusinessValidationException(AddressMessageConstants.DISTRICT_NOT_FOUND, "DISTRICT_NOT_FOUND"));
        GhnWard ward = wardRepository.findById(cmd.getWardCode())
                .orElseThrow(() -> new BusinessValidationException(AddressMessageConstants.WARD_NOT_FOUND, "WARD_NOT_FOUND"));

        if (!district.getProvince().getProvinceId().equals(province.getProvinceId())) {
            throw new BusinessValidationException(AddressMessageConstants.INVALID_DISTRICT_PROVINCE, "INVALID_DISTRICT_PROVINCE");
        }
        if (!ward.getDistrict().getDistrictId().equals(district.getDistrictId())) {
            throw new BusinessValidationException(AddressMessageConstants.INVALID_WARD_DISTRICT, "INVALID_WARD_DISTRICT");
        }

        UserAddress address = UserAddress.builder()
                .user(user)
                .recipientPhone(cmd.getRecipientPhone())
                .province(province)
                .district(district)
                .ward(ward)
                .streetAddress(cmd.getStreetAddress())
                .addressLabel(cmd.getAddressLabel() != null && !cmd.getAddressLabel().isBlank() ? cmd.getAddressLabel() : "Nhà riêng")
                .lastUsedAt(LocalDateTime.now())
                .build();
        userAddressRepository.save(address);

        // Create and save Cart for new User
        Cart cart = Cart.builder()
                .user(user)
                .build();
        cartRepository.save(cart);

        // 6. Assign Customer Role
        Role customerRole = roleRepository.findByRoleCode("CUSTOMER")
                .orElseThrow(() -> new BusinessValidationException("Role CUSTOMER not found in database", "ROLE_NOT_FOUND"));

        UserRole userRole = UserRole.builder()
                .user(user)
                .role(customerRole)
                .build();
        userRoleRepository.save(userRole);

        // Grant direct Order permission to the user
        Permission orderPermission = permissionRepository.findByPermissionCode(com.inkpulse.constants.PermissionConstants.BookEditions.ORDER)
                .orElseThrow(() -> new BusinessValidationException("Không tìm thấy quyền đặt hàng", "PERMISSION_NOT_FOUND"));

        UserPermission userPermission = UserPermission.builder()
                .user(user)
                .permission(orderPermission)
                .build();
        userPermissionRepository.save(userPermission);

        // 7. Register and save User Device
        UUID deviceUuid;
        try {
            deviceUuid = (deviceIdStr != null && !deviceIdStr.isBlank())
                    ? UUID.fromString(deviceIdStr)
                    : UUID.randomUUID();
        } catch (IllegalArgumentException e) {
            deviceUuid = UUID.randomUUID();
        }

        DeviceType devType = DeviceType.DESKTOP;
        if (cmd.getDeviceType() != null) {
            try {
                devType = DeviceType.valueOf(cmd.getDeviceType().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        UserDevice device = UserDevice.builder()
                .user(user)
                .deviceName(cmd.getDeviceName() != null ? cmd.getDeviceName() : "Unknown Device")
                .deviceType(devType)
                .browserFingerprint(cmd.getBrowserFingerprint())
                .lastLoginAt(LocalDateTime.now())
                .trusted(true) // device that registered is trusted
                .build();
        device = userDeviceRepository.save(device);

        // 8. Store user session details
        List<String> roles = List.of(customerRole.getRoleCode());
        List<String> permissions = tokenService.collectPermissionsFromDb(user.getId());

        tokenService.storeUserSession(
                user.getId(),
                roles,
                permissions,
                setting.getDisplayMode(),
                setting.getChoiceLanguage()
        );

        // Eager cache cart items
        tokenService.cacheUserCart(user.getId());

        // 9. Generate and return Access & Refresh tokens
        String accessToken = tokenService.generateAccessToken(user.getId(), user.getUsername(), roles);
        String refreshToken = tokenService.generateRefreshToken(user.getId(), device.getId());

        log.info("Registration verified successfully. User created: {}", user.getUsername());

        return LoginResult.builder()
                .mfaRequired(false)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
