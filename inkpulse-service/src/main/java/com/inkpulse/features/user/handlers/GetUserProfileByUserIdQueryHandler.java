package com.inkpulse.features.user.handlers;

import com.inkpulse.cache.SectionCacheService;
import com.inkpulse.corehelpers.exceptions.ResourceNotFoundException;
import com.inkpulse.entities.User;
import com.inkpulse.entities.UserProfile;
import com.inkpulse.features.user.dto.UserProfileCacheDto;
import com.inkpulse.features.user.queries.GetUserProfileByUserIdQuery;
import com.inkpulse.repositories.UserRepository;
import com.inkpulse.repositories.MfaConfigRepository;
import com.inkpulse.cqrs.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetUserProfileByUserIdQueryHandler implements Query.QueryHandler<GetUserProfileByUserIdQuery, UserProfileCacheDto> {

    private final UserRepository userRepository;
    private final MfaConfigRepository mfaConfigRepository;
    private final SectionCacheService sectionCache;

    @Override
    @Transactional(readOnly = true)
    public UserProfileCacheDto handle(GetUserProfileByUserIdQuery query) {
        String userIdStr = query.userId().toString();

        // 1. Try to read from cache (Cache-Aside Pattern)
        UserProfileCacheDto cachedProfile = sectionCache.get(userIdStr, UserProfileCacheDto.class);
        if (cachedProfile != null) {
            log.debug("Cache hit for user profile of user: {}", userIdStr);
            return cachedProfile;
        }

        log.debug("Cache miss for user profile of user: {}. Fetching from DB...", userIdStr);

        // 2. Cache miss: Fetch from database
        User user = userRepository.findById(query.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", query.userId()));

        UserProfile profile = user.getProfile();
        if (profile == null) {
            throw new ResourceNotFoundException("UserProfile", "userId", query.userId());
        }

        var setting = user.getSetting();
        String displayMode = (setting != null && setting.getDisplayMode() != null) 
                ? setting.getDisplayMode().name() 
                : "SYSTEM";
        String choiceLanguage = (setting != null && setting.getChoiceLanguage() != null) 
                ? setting.getChoiceLanguage().name() 
                : "VI";

        // Fetch active MFA configuration types for the user
        List<String> mfaTypes = mfaConfigRepository.findAllByUserId(query.userId()).stream()
                .map(config -> config.getMfaType().getTypeName().name())
                .toList();

        // 3. Map to DTO
        List<UserProfileCacheDto.UserAddressCacheDto> addressDtos = user.getAddresses().stream()
                .filter(addr -> !addr.isDeleted())
                .sorted((a, b) -> {
                    LocalDateTime atA = a.getLastUsedAt();
                    LocalDateTime atB = b.getLastUsedAt();
                    if (atA == null && atB == null) {
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    }
                    if (atA == null) return 1;
                    if (atB == null) return -1;
                    int comp = atB.compareTo(atA);
                    return comp != 0 ? comp : b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .map(addr -> new UserProfileCacheDto.UserAddressCacheDto(
                        addr.getId().toString(),
                        addr.getRecipientPhone(),
                        addr.getProvince().getProvinceId(),
                        addr.getProvince().getProvinceName(),
                        addr.getDistrict().getDistrictId(),
                        addr.getDistrict().getDistrictName(),
                        addr.getWard().getWardCode(),
                        addr.getWard().getWardName(),
                        addr.getStreetAddress(),
                        addr.getAddressLabel(),
                        addr.getLastUsedAt()
                ))
                .toList();

        UserProfileCacheDto profileDto = new UserProfileCacheDto(
                user.getId().toString(),
                user.getUsername(),
                user.getEmail(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getFullName(),
                profile.getGender() != null ? profile.getGender().name() : "UNKNOWN",
                profile.getDob(),
                profile.getAvatarUrl(),
                profile.getBiography(),
                profile.getTimezone(),
                displayMode,
                choiceLanguage,
                user.isMfaEnabled(),
                mfaTypes,
                addressDtos
        );

        // 4. Save to cache for subsequent reads
        sectionCache.set(profileDto);
        log.debug("Saved user profile to cache for user: {}", userIdStr);

        return profileDto;
    }
}
