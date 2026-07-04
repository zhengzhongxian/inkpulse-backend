package com.inkpulse.features.auth.dto;

import com.inkpulse.cache.CacheSection;
import com.inkpulse.cache.Cacheable;
import com.inkpulse.constants.KeyConstants;

/**
 * Represents a blacklisted access token stored in Redis.
 *
 * Key: bl:{jti}   (jti = JWT ID claim, unique per token)
 * TTL: matches access-token-ttl (15 min) — auto-expires from Redis
 */
@CacheSection(KeyConstants.SECTION_BLACKLISTED_TOKENS)
public record BlacklistedTokenDto(
        String jti,
        String userId,
        String reasonCode
) implements Cacheable {

    @Override
    public String cacheId() {
        return jti;
    }
}
