package com.inkpulse.features.auth.dto;

import com.inkpulse.cache.CacheSection;
import com.inkpulse.cache.Cacheable;
import com.inkpulse.constants.KeyConstants;
import java.util.UUID;

@CacheSection(KeyConstants.SECTION_REFRESH_TOKENS)
public record RefreshTokenDto(
    String tokenHash,
    UUID userId,
    UUID deviceId,
    boolean isRevoked,
    String parentTokenId
) implements Cacheable {
    @Override
    public String cacheId() {
        return tokenHash;
    }
}
