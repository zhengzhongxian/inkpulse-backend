package com.inkpulse.features.auth.dto;

import com.inkpulse.cache.CacheSection;
import com.inkpulse.cache.Cacheable;
import com.inkpulse.constants.KeyConstants;
import java.time.LocalDateTime;

@CacheSection(KeyConstants.SECTION_REGISTER_OTP_SESSION)
public record RegisterOtpSessionDto(
    String email,
    String otpCode,
    LocalDateTime nextRetryAt,
    int failCount
) implements Cacheable {
    @Override
    public String cacheId() {
        return email;
    }
}
