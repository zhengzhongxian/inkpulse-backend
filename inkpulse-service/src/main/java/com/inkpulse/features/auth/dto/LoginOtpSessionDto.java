package com.inkpulse.features.auth.dto;

import com.inkpulse.cache.CacheSection;
import com.inkpulse.cache.Cacheable;
import com.inkpulse.constants.KeyConstants;

@CacheSection(KeyConstants.SECTION_LOGIN_OTP_SESSION)
public record LoginOtpSessionDto(
    String email,
    String otpCode
) implements Cacheable {
    @Override
    public String cacheId() {
        return email;
    }
}
