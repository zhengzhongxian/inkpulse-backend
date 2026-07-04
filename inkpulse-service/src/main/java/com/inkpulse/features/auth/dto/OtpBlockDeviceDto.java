package com.inkpulse.features.auth.dto;

import com.inkpulse.cache.CacheSection;
import com.inkpulse.cache.Cacheable;
import com.inkpulse.constants.KeyConstants;

@CacheSection(KeyConstants.SECTION_OTP_BLOCK_DEVICE)
public record OtpBlockDeviceDto(
    String deviceId,
    boolean blocked
) implements Cacheable {
    @Override
    public String cacheId() {
        return deviceId;
    }
}
