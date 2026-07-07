package com.inkpulse.features.address.handlers;

import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.GhnProvince;
import com.inkpulse.models.response.ghn.GhnProvinceResponse;
import com.inkpulse.features.address.queries.GetGhnProvincesQuery;
import com.inkpulse.repositories.GhnProvinceRepository;
import com.inkpulse.cache.ICacheService;
import com.inkpulse.cache.CacheProperties;
import com.inkpulse.constants.KeyConstants;
import com.inkpulse.features.address.handlers.common.AddressCommonHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetGhnProvincesQueryHandler implements Query.QueryHandler<GetGhnProvincesQuery, List<GhnProvinceResponse>> {

    private final GhnProvinceRepository provinceRepository;
    private final ICacheService cacheService;
    private final CacheProperties cacheProperties;
    private final AddressCommonHelper addressCommonHelper;

    @Value("${" + KeyConstants.GHN_LOCK_RETRY_TIMEOUT + ":5}")
    private int retryTimeoutSeconds;

    @Value("${" + KeyConstants.GHN_LOCK_RETRY_INTERVAL + ":100}")
    private int retryIntervalMs;

    @Override
    @Transactional(readOnly = true)
    public List<GhnProvinceResponse> handle(GetGhnProvincesQuery query) {
        log.info("Handling GetGhnProvincesQuery");

        addressCommonHelper.waitForInitLock();

        CacheProperties.SectionConfig section = cacheProperties.getSections().get(KeyConstants.SECTION_GHN_PROVINCES);
        if (section == null) {
            throw new IllegalStateException("Cache section '" + KeyConstants.SECTION_GHN_PROVINCES + "' is not configured in application.yml");
        }

        String cacheKey = section.getKey();
        String lockKey = "lock:" + cacheKey;
        Duration cacheTtl = Duration.ofMinutes(section.getTtl());
        Duration lockTtl = Duration.ofSeconds(10);

        GhnProvinceResponse[] cached = cacheService.get(cacheKey, GhnProvinceResponse[].class);
        if (cached != null) {
            log.debug("GHN Provinces cache hit");
            return Arrays.asList(cached);
        }

        log.debug("GHN Provinces cache miss. Attempting to acquire lock.");
        String lockValue = UUID.randomUUID().toString();

        if (cacheService.acquireLock(lockKey, lockValue, lockTtl, true, Duration.ofSeconds(retryTimeoutSeconds),
                Duration.ofMillis(retryIntervalMs))) {
            log.debug("Acquired lock for loading GHN provinces");
            try {
                cached = cacheService.get(cacheKey, GhnProvinceResponse[].class);
                if (cached != null) {
                    log.debug("GHN Provinces cache hit on double-check");
                    return Arrays.asList(cached);
                }

                List<GhnProvince> provinces = provinceRepository.findAll();
                List<GhnProvinceResponse> resultList = provinces.stream()
                        .map(p -> GhnProvinceResponse.builder()
                                .provinceId(p.getProvinceId())
                                .provinceName(p.getProvinceName())
                                .provinceCode(p.getProvinceCode())
                                .build())
                        .sorted((a, b) -> a.getProvinceName().compareToIgnoreCase(b.getProvinceName()))
                        .toList();

                if (!resultList.isEmpty()) {
                    cacheService.set(cacheKey, resultList.toArray(new GhnProvinceResponse[0]), cacheTtl);
                    log.debug("GHN Provinces loaded from DB and saved to cache");
                }
                return resultList;

            } catch (Exception e) {
                log.error("Error occurred while loading GHN provinces from database", e);
                throw e;
            } finally {
                boolean released = cacheService.releaseLock(lockKey, lockValue);
                log.debug("Released GHN provinces lock: {}", released);
            }
        } else {
            log.debug("Failed to acquire lock. Falling back to direct database retrieval.");
            List<GhnProvince> provinces = provinceRepository.findAll();
            return provinces.stream()
                    .map(p -> GhnProvinceResponse.builder()
                            .provinceId(p.getProvinceId())
                            .provinceName(p.getProvinceName())
                            .provinceCode(p.getProvinceCode())
                            .build())
                    .sorted((a, b) -> a.getProvinceName().compareToIgnoreCase(b.getProvinceName()))
                    .toList();
        }
    }
}
