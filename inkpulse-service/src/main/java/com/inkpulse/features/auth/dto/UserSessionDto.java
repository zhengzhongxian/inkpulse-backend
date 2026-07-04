package com.inkpulse.features.auth.dto;

import com.inkpulse.cache.CacheSection;
import com.inkpulse.cache.Cacheable;
import com.inkpulse.constants.KeyConstants;
import java.util.List;

@CacheSection(KeyConstants.SECTION_USER_SESSION)
public record UserSessionDto(
    String userId,
    List<String> roles,
    List<String> permissions,
    String displayMode,
    String choiceLanguage
) implements Cacheable {
    @Override
    public String cacheId() {
        return userId;
    }
}
