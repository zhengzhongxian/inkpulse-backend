package com.inkpulse.features.user.handlers;

import com.inkpulse.cache.SectionCacheService;
import com.inkpulse.constants.AppConstants;
import com.inkpulse.constants.message.UserMessageConstants;
import com.inkpulse.corehelpers.exceptions.BusinessValidationException;
import com.inkpulse.cqrs.Command;
import com.inkpulse.entities.*;
import com.inkpulse.entities.enums.*;
import com.inkpulse.features.auth.service.TokenService;
import com.inkpulse.features.user.commands.UpdateUserProfileCommand;
import com.inkpulse.features.user.dto.UserProfileCacheDto;
import com.inkpulse.features.user.queries.GetUserProfileByUserIdQuery;
import com.inkpulse.repositories.*;
import com.inkpulse.service.minio.IMinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import an.awesome.pipelinr.Pipeline;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateUserProfileCommandHandler implements Command.CommandHandler<UpdateUserProfileCommand, UserProfileCacheDto> {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserSettingRepository userSettingRepository;
    private final MfaConfigRepository mfaConfigRepository;
    private final MfaTypeLookupRepository mfaTypeLookupRepository;
    private final IMinioService minioService;
    private final SectionCacheService sectionCache;
    private final TokenService tokenService;
    private final Pipeline pipeline;

    @Override
    @Transactional
    public UserProfileCacheDto handle(UpdateUserProfileCommand cmd) {
        User user = userRepository.findById(cmd.getUserId())
                .orElseThrow(() -> new BusinessValidationException(UserMessageConstants.USER_NOT_FOUND, "USER_NOT_FOUND"));

        UserProfile profile = user.getProfile();
        if (profile == null) {
            profile = UserProfile.builder().user(user).build();
        }

        // 1. Update basic profile info
        profile.setFirstName(cmd.getFirstName());
        profile.setLastName(cmd.getLastName());
        
        String cleanFirst = cmd.getFirstName() != null ? cmd.getFirstName().trim() : "";
        String cleanLast = cmd.getLastName() != null ? cmd.getLastName().trim() : "";
        profile.setFullName((cleanFirst + " " + cleanLast).trim());
        
        if (cmd.getGender() != null) {
            try {
                profile.setGender(Gender.valueOf(cmd.getGender().toUpperCase()));
            } catch (IllegalArgumentException e) {
                profile.setGender(Gender.UNKNOWN);
            }
        }
        profile.setDob(cmd.getDob());
        profile.setBiography(cmd.getBiography());

        // 2. Upload avatar to MinIO if provided
        if (cmd.getAvatarFile() != null) {
            try {
                String avatarExt = ".jpg";
                String originalName = cmd.getAvatarFile().getFileName();
                if (originalName != null && originalName.lastIndexOf('.') != -1) {
                    avatarExt = originalName.substring(originalName.lastIndexOf('.'));
                }
                String objectName = "users/avatars/" + user.getId().toString() + avatarExt;
                
                // Upload to MinIO using custom avatar bucket constant
                minioService.uploadFile(
                        cmd.getAvatarFile().getInputStream(),
                        cmd.getAvatarFile().getFileName(),
                        cmd.getAvatarFile().getContentType(),
                        cmd.getAvatarFile().getFileSize(),
                        objectName,
                        AppConstants.MinioBucket.AVATAR,
                        null
                );
                
                // Relative path in DB starts with "avatar/"
                profile.setAvatarUrl("avatar/" + objectName);
            } catch (Exception ex) {
                log.error("Failed to upload avatar to MinIO. User ID: {}", user.getId(), ex);
                throw new BusinessValidationException(UserMessageConstants.AVATAR_UPLOAD_FAILED + ex.getMessage(), "AVATAR_UPLOAD_FAILED");
            }
        }
        userProfileRepository.save(profile);

        // 3. Update settings
        UserSetting setting = user.getSetting();
        if (setting == null) {
            setting = UserSetting.builder().user(user).build();
        }
        if (cmd.getDisplayMode() != null) {
            try {
                setting.setDisplayMode(DisplayMode.valueOf(cmd.getDisplayMode().toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        if (cmd.getChoiceLanguage() != null) {
            try {
                setting.setChoiceLanguage(Language.valueOf(cmd.getChoiceLanguage().toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        userSettingRepository.save(setting);

        // 4. Update MFA Configs if provided in request
        List<String> requestedMfaTypes = cmd.getMfaTypes();
        if (requestedMfaTypes != null) {
            List<MfaConfig> existingConfigs = mfaConfigRepository.findAllByUserId(cmd.getUserId());
            mfaConfigRepository.deleteAll(existingConfigs);

            List<String> activeMfaTypes = requestedMfaTypes.stream()
                    .filter(t -> t != null && !t.isBlank())
                    .toList();

            if (!activeMfaTypes.isEmpty()) {
                boolean first = true;
                for (String typeStr : activeMfaTypes) {
                    try {
                        MfaType mfaTypeEnum = MfaType.valueOf(typeStr.toUpperCase());
                        MfaTypeLookup mfaTypeLookup = mfaTypeLookupRepository.findByTypeName(mfaTypeEnum)
                                .orElseThrow(() -> new BusinessValidationException(UserMessageConstants.MFA_TYPE_NOT_FOUND + typeStr, "MFA_TYPE_NOT_FOUND"));
                        
                        MfaConfig newConfig = MfaConfig.builder()
                                .user(user)
                                .mfaType(mfaTypeLookup)
                                .isDefault(first)
                                .createdAt(LocalDateTime.now())
                                .build();
                        mfaConfigRepository.save(newConfig);
                        first = false;
                    } catch (IllegalArgumentException ignored) {}
                }
                user.setMfaEnabled(true);
            } else {
                user.setMfaEnabled(false);
            }
        }

        // 5. Evict Cache
        sectionCache.remove(user.getId().toString(), UserProfileCacheDto.class);
        tokenService.removeUserSession(user.getId());

        // 6. Return fresh updated profile DTO
        return pipeline.send(new GetUserProfileByUserIdQuery(user.getId()));
    }
}
