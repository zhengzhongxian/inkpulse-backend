package com.inkpulse.features.auth.dto;

import com.inkpulse.cache.CacheSection;
import com.inkpulse.cache.Cacheable;
import com.inkpulse.constants.KeyConstants;
import java.time.LocalDateTime;

@CacheSection(KeyConstants.SECTION_LOGIN_ATTEMPTS)
public record LoginAttemptsDto(
    String login,
    int failCount,
    String lastIp,
    LocalDateTime lastAttemptAt
) implements Cacheable {
    @Override
    public String cacheId() {
        return login;
    }
}
