package com.inkpulse.features.auth.dto;

import com.inkpulse.cache.CacheSection;
import com.inkpulse.cache.Cacheable;
import com.inkpulse.constants.KeyConstants;

@CacheSection(KeyConstants.SECTION_OTP_BLOCK_EMAIL)
public record OtpBlockEmailDto(
    String email,
    boolean blocked
) implements Cacheable {
    @Override
    public String cacheId() {
        return email;
    }
}
