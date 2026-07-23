package com.inkpulse.service.ai;

import com.inkpulse.cache.ICacheService;
import com.inkpulse.constants.KeyConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIVisionRateLimiter {

    private final ICacheService cacheService;

    @Value("${" + KeyConstants.AI_VISION_RATE_LIMIT_MAX + ":20}")
    private int maxPerHour;

    /**
     * Checks if the admin is allowed to call the AI Vision endpoint.
     * Implements a 1-hour fixed window rate limit using Redis.
     *
     * @param adminId Admin User ID
     * @return true if allowed, false if quota is exceeded
     */
    public boolean isAllowed(String adminId) {
        if (adminId == null || adminId.isBlank()) {
            return true; // If somehow userID is empty, fail-open to prevent breaking operations
        }

        String key = KeyConstants.SECTION_AI_VISION_RATE + ":" + adminId;
        Long currentCount = cacheService.increment(key);
        
        if (currentCount == null) {
            return true;
        }

        if (currentCount == 1L) {
            // First request in the window, set expiration to 1 hour
            cacheService.expire(key, Duration.ofHours(1));
        }

        log.info("AI Vision quota check for Admin: {}. Count: {}/{}", adminId, currentCount, maxPerHour);

        return currentCount <= maxPerHour;
    }

    /**
     * Gets the remaining calls for the admin in the current 1-hour window.
     *
     * @param adminId Admin User ID
     * @return remaining count
     */
    public long getRemainingQuota(String adminId) {
        if (adminId == null || adminId.isBlank()) {
            return maxPerHour;
        }

        String key = KeyConstants.SECTION_AI_VISION_RATE + ":" + adminId;
        String countStr = cacheService.getString(key);
        
        if (countStr == null) {
            return maxPerHour;
        }

        try {
            long count = Long.parseLong(countStr);
            long remaining = maxPerHour - count;
            return Math.max(0, remaining);
        } catch (NumberFormatException e) {
            return maxPerHour;
        }
    }
}
