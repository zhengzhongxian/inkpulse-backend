package com.inkpulse.features.auth.dto;

import com.inkpulse.cache.CacheSection;
import com.inkpulse.cache.Cacheable;
import com.inkpulse.constants.KeyConstants;
import java.time.LocalDateTime;

@CacheSection(KeyConstants.SECTION_ACCOUNT_LOCK)
public record AccountLockDto(
    String userId,
    String blockReason,
    LocalDateTime blockedAt,
    LocalDateTime unlockAt
) implements Cacheable {
    @Override
    public String cacheId() {
        return userId;
    }
}
