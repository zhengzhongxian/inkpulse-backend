package com.inkpulse.features.address.handlers;

import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.GhnWard;
import com.inkpulse.models.response.GhnWardResponse;
import com.inkpulse.features.address.queries.GetGhnWardsQuery;
import com.inkpulse.repositories.GhnWardRepository;
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
public class GetGhnWardsQueryHandler implements Query.QueryHandler<GetGhnWardsQuery, List<GhnWardResponse>> {

    private final GhnWardRepository wardRepository;
    private final ICacheService cacheService;
    private final CacheProperties cacheProperties;
    private final AddressCommonHelper addressCommonHelper;

    @Value("${" + KeyConstants.GHN_LOCK_RETRY_TIMEOUT + ":5}")
    private int retryTimeoutSeconds;

    @Value("${" + KeyConstants.GHN_LOCK_RETRY_INTERVAL + ":100}")
    private int retryIntervalMs;

    @Override
    @Transactional(readOnly = true)
    public List<GhnWardResponse> handle(GetGhnWardsQuery query) {
        log.info("Handling GetGhnWardsQuery for districtId: {}", query.districtId());

        addressCommonHelper.waitForInitLock();

        CacheProperties.SectionConfig section = cacheProperties.getSections().get(KeyConstants.SECTION_GHN_WARDS);
        if (section == null) {
            throw new IllegalStateException(
                    "Cache section '" + KeyConstants.SECTION_GHN_WARDS + "' is not configured in application.yml");
        }

        String cacheKey = section.getKey() + query.districtId();
        String lockKey = "lock:" + cacheKey;
        Duration cacheTtl = Duration.ofMinutes(section.getTtl());
        Duration lockTtl = Duration.ofSeconds(10);

        GhnWardResponse[] cached = cacheService.get(cacheKey, GhnWardResponse[].class);
        if (cached != null) {
            log.debug("GHN Wards cache hit for districtId: {}", query.districtId());
            return Arrays.asList(cached);
        }

        log.debug("GHN Wards cache miss. Attempting to acquire lock.");
        String lockValue = UUID.randomUUID().toString();

        if (cacheService.acquireLock(lockKey, lockValue, lockTtl, true, Duration.ofSeconds(retryTimeoutSeconds),
                Duration.ofMillis(retryIntervalMs))) {
            log.debug("Acquired lock for loading GHN wards");
            try {
                cached = cacheService.get(cacheKey, GhnWardResponse[].class);
                if (cached != null) {
                    log.debug("GHN Wards cache hit on double-check");
                    return Arrays.asList(cached);
                }

                List<GhnWard> wards = wardRepository.findByDistrictDistrictId(query.districtId());
                List<GhnWardResponse> resultList = wards.stream()
                        .map(w -> GhnWardResponse.builder()
                                .wardCode(w.getWardCode())
                                .districtId(w.getDistrict().getDistrictId())
                                .wardName(w.getWardName())
                                .build())
                        .sorted((a, b) -> a.getWardName().compareToIgnoreCase(b.getWardName()))
                        .toList();

                cacheService.set(cacheKey, resultList.toArray(new GhnWardResponse[0]), cacheTtl);
                log.debug("GHN Wards loaded from DB and saved to cache");
                return resultList;

            } catch (Exception e) {
                log.error("Error occurred while loading GHN wards from database", e);
                throw e;
            } finally {
                boolean released = cacheService.releaseLock(lockKey, lockValue);
                log.debug("Released GHN wards lock: {}", released);
            }
        } else {
            log.debug("Failed to acquire lock. Falling back to direct database retrieval.");
            List<GhnWard> wards = wardRepository.findByDistrictDistrictId(query.districtId());
            return wards.stream()
                    .map(w -> GhnWardResponse.builder()
                            .wardCode(w.getWardCode())
                            .districtId(w.getDistrict().getDistrictId())
                            .wardName(w.getWardName())
                            .build())
                    .sorted((a, b) -> a.getWardName().compareToIgnoreCase(b.getWardName()))
                    .toList();
        }
    }
}
