package com.inkpulse.features.auth.dto;

import com.inkpulse.cache.CacheSection;
import com.inkpulse.cache.Cacheable;
import com.inkpulse.constants.KeyConstants;
import java.time.LocalDateTime;
import java.util.UUID;

@CacheSection(KeyConstants.SECTION_FORGOT_PASSWORD_SESSION)
public record ForgotPasswordSessionDto(
    String token,
    UUID userId,
    String email,
    LocalDateTime nextRetryAt
) implements Cacheable {
    @Override
    public String cacheId() {
        return token;
    }
}
