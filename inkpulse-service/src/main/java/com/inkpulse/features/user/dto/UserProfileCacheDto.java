package com.inkpulse.features.user.dto;

import com.inkpulse.cache.CacheSection;
import com.inkpulse.cache.Cacheable;
import com.inkpulse.constants.KeyConstants;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@CacheSection(KeyConstants.SECTION_USER_PROFILE)
public record UserProfileCacheDto(
    String userId,
    String username,
    String email,
    String firstName,
    String lastName,
    String fullName,
    String gender,
    LocalDate dob,
    String avatarUrl,
    String biography,
    String timezone,
    String displayMode,
    String choiceLanguage,
    boolean mfaEnabled,
    List<String> mfaTypes,
    Long coinBalance,
    List<UserAddressCacheDto> addresses
) implements Cacheable {
    @Override
    public String cacheId() {
        return userId;
    }

    public record UserAddressCacheDto(
        String id,
        String recipientPhone,
        Integer provinceId,
        String provinceName,
        Integer districtId,
        String districtName,
        String wardCode,
        String wardName,
        String streetAddress,
        String addressLabel,
        LocalDateTime lastUsedAt
    ) {}
}
