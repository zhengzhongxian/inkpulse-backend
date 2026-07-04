package com.inkpulse.features.address.handlers.common;

import com.inkpulse.cache.ICacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AddressCommonHelper {

    private final ICacheService cacheService;

    public void waitForInitLock() {
        String globalLockKey = "lock:ghn:init";
        long startTime = System.currentTimeMillis();
        long timeoutMs = 180000; // 3 minutes timeout

        while (cacheService.getString(globalLockKey) != null) {
            log.info("GHN crawl lock is active. Waiting for crawl to complete...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for GHN master data initialization", e);
            }
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                throw new RuntimeException("Timeout waiting for GHN master data initialization");
            }
        }
    }
}
