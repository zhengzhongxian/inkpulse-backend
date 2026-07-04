package com.inkpulse.features.auth.dto;

import com.inkpulse.cache.CacheSection;
import com.inkpulse.cache.Cacheable;
import com.inkpulse.constants.KeyConstants;

@CacheSection(KeyConstants.SECTION_OTP_LIMIT_DEVICE)
public record OtpLimitDeviceDto(
    String deviceId,
    int requestCount
) implements Cacheable {
    @Override
    public String cacheId() {
        return deviceId;
    }
}
