package com.inkpulse.features.auth.handlers;

import com.inkpulse.cache.SectionCacheService;
import com.inkpulse.constants.PermissionConstants;
import com.inkpulse.constants.message.AddressMessageConstants;
import com.inkpulse.constants.message.AuthMessageConstants;
import com.inkpulse.constants.message.RegisterMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.entities.*;
import com.inkpulse.entities.enums.DeviceType;
import com.inkpulse.entities.enums.DisplayMode;
import com.inkpulse.entities.enums.Gender;
import com.inkpulse.entities.enums.Language;
import com.inkpulse.entities.enums.UserStatus;
import com.inkpulse.features.auth.commands.GoogleRegisterCommand;
import com.inkpulse.features.auth.service.TokenService;
import com.inkpulse.models.response.auth.LoginResult;
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
public class GoogleRegisterHandler implements Command.CommandHandler<GoogleRegisterCommand, LoginResult> {

    private final UserRepository userRepository;
    private final UserSocialAccountRepository userSocialAccountRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final UserSettingRepository userSettingRepository;
    private final CartRepository cartRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    
    private final GhnProvinceRepository provinceRepository;
    private final GhnDistrictRepository districtRepository;
    private final GhnWardRepository wardRepository;
    private final UserAddressRepository userAddressRepository;
    private final TokenService tokenService;

    @Override
    @Transactional
    public LoginResult handle(GoogleRegisterCommand cmd) {
        // 1. Uniqueness check for username, email and googleUserId
        if (userRepository.existsByUsername(cmd.getUsername())) {
            throw new BusinessValidationException(RegisterMessageConstants.USERNAME_TAKEN, "USERNAME_TAKEN");
        }
        if (userRepository.findByEmail(cmd.getEmail()).isPresent()) {
            throw new BusinessValidationException(RegisterMessageConstants.EMAIL_TAKEN, "EMAIL_TAKEN");
        }
        if (userSocialAccountRepository.findByProviderAndProviderKey("GOOGLE", cmd.getGoogleUserId()).isPresent()) {
            throw new BusinessValidationException(AuthMessageConstants.GOOGLE_ACCOUNT_ALREADY_LINKED, "SOCIAL_ALREADY_LINKED");
        }

        // 2. Create User (password = null)
        User user = User.builder()
                .username(cmd.getUsername())
                .email(cmd.getEmail())
                .status(UserStatus.ACTIVE)
                .verified(true)
                .build();

        // 3. Persist User first to generate ID (do NOT manually assign ID)
        user = userRepository.save(user);

        // 4. Create UserProfile
        Gender gender = Gender.OTHER;
        try {
            if (cmd.getGender() != null) {
                gender = Gender.valueOf(cmd.getGender().toUpperCase());
            }
        } catch (IllegalArgumentException ignored) {}

        String fullName = (cmd.getFirstName() != null ? cmd.getFirstName().trim() : "") + " "
                + (cmd.getLastName() != null ? cmd.getLastName().trim() : "");
        fullName = fullName.trim();
        if (fullName.isEmpty()) {
            fullName = cmd.getName();
        }

        UserProfile profile = UserProfile.builder()
                .user(user)
                .firstName(cmd.getFirstName())
                .lastName(cmd.getLastName())
                .fullName(fullName.isEmpty() ? null : fullName)
                .gender(gender)
                .dob(cmd.getDob())
                .avatarUrl(cmd.getPicture())
                .build();

        // 5. Create UserSetting
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
        user = userRepository.save(user);

        // 6. Create UserSocialAccount link
        UserSocialAccount social = UserSocialAccount.builder()
                .user(user)
                .provider("GOOGLE")
                .providerKey(cmd.getGoogleUserId())
                .build();
        userSocialAccountRepository.save(social);

        // 7. Validate and save delivery Address
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

        // 8. Create Cart
        Cart cart = Cart.builder()
                .user(user)
                .build();
        cartRepository.save(cart);

        // 9. Assign CUSTOMER role
        Role customerRole = roleRepository.findByRoleCode("CUSTOMER")
                .orElseThrow(() -> new BusinessValidationException("Role CUSTOMER not found in database", "ROLE_NOT_FOUND"));

        UserRole userRole = UserRole.builder()
                .user(user)
                .role(customerRole)
                .build();
        userRoleRepository.save(userRole);

        // 10. Assign ORDER permission
        Permission orderPermission = permissionRepository.findByPermissionCode(PermissionConstants.BookEditions.ORDER)
                .orElseThrow(() -> new BusinessValidationException("Không tìm thấy quyền đặt hàng", "PERMISSION_NOT_FOUND"));

        UserPermission userPermission = UserPermission.builder()
                .user(user)
                .permission(orderPermission)
                .build();
        userPermissionRepository.save(userPermission);

        // 11. Register Device
        UUID deviceId = parseDeviceId(cmd.getDeviceId());
        UserDevice device = validateAndSyncDevice(user, deviceId, cmd.getBrowserFingerprint(),
                cmd.getDeviceName(), cmd.getDeviceType());

        // 12. Issue tokens and eager caching
        List<String> rolesList = List.of(customerRole.getRoleCode());
        List<String> permissionsList = List.of(orderPermission.getPermissionCode());

        tokenService.storeUserSession(
                user.getId(), rolesList, permissionsList,
                setting.getDisplayMode(), setting.getChoiceLanguage()
        );

        tokenService.cacheUserCart(user.getId());

        String accessToken = tokenService.generateAccessToken(user.getId(), user.getUsername(), rolesList);
        String refreshToken = tokenService.generateRefreshToken(user.getId(), device.getId());

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        device.setLastLoginAt(LocalDateTime.now());
        userDeviceRepository.save(device);

        log.info("Google user registered and logged in: {}", user.getUsername());
        return LoginResult.builder()
                .mfaRequired(false)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
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
}
