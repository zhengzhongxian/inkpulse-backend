package com.inkpulse.features.auth.dto;

import com.inkpulse.cache.CacheSection;
import com.inkpulse.cache.Cacheable;
import com.inkpulse.constants.KeyConstants;

@CacheSection(KeyConstants.SECTION_OTP_LIMIT_EMAIL)
public record OtpLimitEmailDto(
    String email,
    int requestCount
) implements Cacheable {
    @Override
    public String cacheId() {
        return email;
    }
}
