package com.inkpulse.features.address.handlers;

import com.inkpulse.cqrs.Query;
import com.inkpulse.entities.GhnDistrict;
import com.inkpulse.models.response.ghn.GhnDistrictResponse;
import com.inkpulse.features.address.queries.GetGhnDistrictsQuery;
import com.inkpulse.repositories.GhnDistrictRepository;
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
public class GetGhnDistrictsQueryHandler
        implements Query.QueryHandler<GetGhnDistrictsQuery, List<GhnDistrictResponse>> {

    private final GhnDistrictRepository districtRepository;
    private final ICacheService cacheService;
    private final CacheProperties cacheProperties;
    private final AddressCommonHelper addressCommonHelper;

    @Value("${" + KeyConstants.GHN_LOCK_RETRY_TIMEOUT + ":5}")
    private int retryTimeoutSeconds;

    @Value("${" + KeyConstants.GHN_LOCK_RETRY_INTERVAL + ":100}")
    private int retryIntervalMs;

    @Override
    @Transactional(readOnly = true)
    public List<GhnDistrictResponse> handle(GetGhnDistrictsQuery query) {
        log.info("Handling GetGhnDistrictsQuery for provinceId: {}", query.provinceId());

        addressCommonHelper.waitForInitLock();

        CacheProperties.SectionConfig section = cacheProperties.getSections().get(KeyConstants.SECTION_GHN_DISTRICTS);
        if (section == null) {
            throw new IllegalStateException(
                    "Cache section '" + KeyConstants.SECTION_GHN_DISTRICTS + "' is not configured in application.yml");
        }

        String cacheKey = section.getKey() + query.provinceId();
        String lockKey = "lock:" + cacheKey;
        Duration cacheTtl = Duration.ofMinutes(section.getTtl());
        Duration lockTtl = Duration.ofSeconds(10);

        GhnDistrictResponse[] cached = cacheService.get(cacheKey, GhnDistrictResponse[].class);
        if (cached != null) {
            log.debug("GHN Districts cache hit for provinceId: {}", query.provinceId());
            return Arrays.asList(cached);
        }

        log.debug("GHN Districts cache miss. Attempting to acquire lock.");
        String lockValue = UUID.randomUUID().toString();

        if (cacheService.acquireLock(lockKey, lockValue, lockTtl, true, Duration.ofSeconds(retryTimeoutSeconds),
                Duration.ofMillis(retryIntervalMs))) {
            log.debug("Acquired lock for loading GHN districts");
            try {
                cached = cacheService.get(cacheKey, GhnDistrictResponse[].class);
                if (cached != null) {
                    log.debug("GHN Districts cache hit on double-check");
                    return Arrays.asList(cached);
                }

                List<GhnDistrict> districts = districtRepository.findByProvinceProvinceId(query.provinceId());
                List<GhnDistrictResponse> resultList = districts.stream()
                        .map(d -> GhnDistrictResponse.builder()
                                .districtId(d.getDistrictId())
                                .provinceId(d.getProvince().getProvinceId())
                                .districtName(d.getDistrictName())
                                .districtCode(d.getDistrictCode())
                                .supportType(d.getSupportType())
                                .build())
                        .sorted((a, b) -> a.getDistrictName().compareToIgnoreCase(b.getDistrictName()))
                        .toList();

                cacheService.set(cacheKey, resultList.toArray(new GhnDistrictResponse[0]), cacheTtl);
                log.debug("GHN Districts loaded from DB and saved to cache");
                return resultList;

            } catch (Exception e) {
                log.error("Error occurred while loading GHN districts from database", e);
                throw e;
            } finally {
                boolean released = cacheService.releaseLock(lockKey, lockValue);
                log.debug("Released GHN districts lock: {}", released);
            }
        } else {
            log.debug("Failed to acquire lock. Falling back to direct database retrieval.");
            List<GhnDistrict> districts = districtRepository.findByProvinceProvinceId(query.provinceId());
            return districts.stream()
                    .map(d -> GhnDistrictResponse.builder()
                            .districtId(d.getDistrictId())
                            .provinceId(d.getProvince().getProvinceId())
                            .districtName(d.getDistrictName())
                            .districtCode(d.getDistrictCode())
                            .supportType(d.getSupportType())
                            .build())
                    .sorted((a, b) -> a.getDistrictName().compareToIgnoreCase(b.getDistrictName()))
                    .toList();
        }
    }
}
