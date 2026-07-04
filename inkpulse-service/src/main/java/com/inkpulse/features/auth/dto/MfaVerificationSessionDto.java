package com.inkpulse.features.auth.dto;

import com.inkpulse.cache.CacheSection;
import com.inkpulse.cache.Cacheable;
import com.inkpulse.constants.KeyConstants;
import java.util.List;
import java.util.UUID;

@CacheSection(KeyConstants.SECTION_MFA_SESSION)
public record MfaVerificationSessionDto(
    String sessionId,
    UUID userId,
    UUID deviceId,
    int attemptCount,
    String mfaType,
    String verificationValue,
    List<MfaConfigItem> mfaConfigs,
    // Number-matching: challengeNumber shown on login screen,
    // challengeOptions are the 3 shuffled numbers sent via email
    Integer challengeNumber,
    List<Integer> challengeOptions
) implements Cacheable {
    @Override
    public String cacheId() {
        return sessionId;
    }

    public record MfaConfigItem(
        String type,
        String displayName,
        String configId
    ) {}
}
