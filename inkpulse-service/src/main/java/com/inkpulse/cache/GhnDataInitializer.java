package com.inkpulse.cache;

import com.inkpulse.constants.KeyConstants;
import com.inkpulse.corehelpers.JsonHelper;
import com.inkpulse.entities.GhnDistrict;
import com.inkpulse.entities.GhnProvince;
import com.inkpulse.entities.GhnWard;
import com.inkpulse.repositories.GhnDistrictRepository;
import com.inkpulse.repositories.GhnProvinceRepository;
import com.inkpulse.repositories.GhnWardRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.inkpulse.cache.ICacheService;

@Slf4j
@Component
@RequiredArgsConstructor
public class GhnDataInitializer {

    private final GhnProvinceRepository provinceRepository;
    private final GhnDistrictRepository districtRepository;
    private final GhnWardRepository wardRepository;
    private final ICacheService cacheService;

    @Value("${" + KeyConstants.GHN_API_TOKEN + "}")
    private String apiToken;

    @Value("${" + KeyConstants.GHN_BASE_URL + "}")
    private String baseUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @EventListener(ApplicationReadyEvent.class)
    public void initGhnData() {
        CompletableFuture.runAsync(() -> {
            try {
                if (provinceRepository.count() > 0) {
                    log.info("GHN master data already exists. Skipping crawl.");
                    return;
                }

                log.info("GHN master data is empty. Starting crawl from GHN API...");
                
                String lockKey = "lock:ghn:init";
                String lockValue = "crawling";
                Duration expiry = Duration.ofMinutes(10);
                
                boolean acquired = cacheService.acquireLock(lockKey, lockValue, expiry, false, Duration.ZERO, Duration.ZERO);
                if (!acquired) {
                    log.warn("Another process is already crawling GHN master data.");
                    return;
                }

                try {
                    crawlAndSaveData();
                    log.info("GHN master data crawl completed successfully!");
                } finally {
                    cacheService.releaseLock(lockKey, lockValue);
                }
            } catch (Exception e) {
                log.error("Failed to crawl and initialize GHN master data", e);
            }
        });
    }

    private void crawlAndSaveData() throws Exception {
        // 1. Fetch Provinces
        String provinceUrl = baseUrl + "/shiip/public-api/master-data/province";
        HttpRequest provinceRequest = HttpRequest.newBuilder()
                .uri(URI.create(provinceUrl))
                .header("token", apiToken)
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> provinceResponse = httpClient.send(provinceRequest, HttpResponse.BodyHandlers.ofString());
        if (provinceResponse.statusCode() != 200) {
            throw new RuntimeException("Failed to fetch provinces from GHN. Status: " + provinceResponse.statusCode());
        }

        GhnProvinceApiResponse provinceApiResponse = JsonHelper.deserializeSafe(provinceResponse.body(), GhnProvinceApiResponse.class);
        if (provinceApiResponse == null || provinceApiResponse.data() == null) {
            throw new RuntimeException("Empty or invalid province response from GHN");
        }

        List<GhnProvince> provinces = new ArrayList<>();
        for (GhnProvinceRaw raw : provinceApiResponse.data()) {
            provinces.add(GhnProvince.builder()
                    .provinceId(raw.ProvinceID())
                    .provinceName(raw.ProvinceName())
                    .provinceCode(raw.Code())
                    .build());
        }

        provinceRepository.saveAll(provinces);
        log.info("Saved {} provinces", provinces.size());

        // 2. Fetch Districts for each Province
        List<GhnDistrict> allDistricts = new ArrayList<>();
        String districtUrl = baseUrl + "/shiip/public-api/master-data/district";

        for (GhnProvince province : provinces) {
            try {
                String reqBody = JsonHelper.serializeSafe(Map.of("province_id", province.getProvinceId()));
                HttpRequest districtRequest = HttpRequest.newBuilder()
                        .uri(URI.create(districtUrl))
                        .header("token", apiToken)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(reqBody))
                        .build();

                HttpResponse<String> districtResponse = httpClient.send(districtRequest, HttpResponse.BodyHandlers.ofString());
                if (districtResponse.statusCode() != 200) {
                    log.warn("Failed to fetch districts for province {}. Status: {}", province.getProvinceId(), districtResponse.statusCode());
                    continue;
                }

                GhnDistrictApiResponse districtApiResponse = JsonHelper.deserializeSafe(districtResponse.body(), GhnDistrictApiResponse.class);
                if (districtApiResponse != null && districtApiResponse.data() != null) {
                    for (GhnDistrictRaw raw : districtApiResponse.data()) {
                        allDistricts.add(GhnDistrict.builder()
                                .districtId(raw.DistrictID())
                                .province(province)
                                .districtName(raw.DistrictName())
                                .districtCode(raw.Code())
                                .supportType(raw.SupportType())
                                .build());
                    }
                }
                // Small delay to prevent rate limit
                Thread.sleep(50);
            } catch (Exception e) {
                log.error("Error fetching districts for province ID: " + province.getProvinceId(), e);
            }
        }

        districtRepository.saveAll(allDistricts);
        log.info("Saved {} districts", allDistricts.size());

        // 3. Fetch Wards for each District
        List<GhnWard> allWards = new ArrayList<>();
        String wardUrl = baseUrl + "/shiip/public-api/master-data/ward?district_id";
        int districtCounter = 0;

        for (GhnDistrict district : allDistricts) {
            try {
                String reqBody = JsonHelper.serializeSafe(Map.of("district_id", district.getDistrictId()));
                HttpRequest wardRequest = HttpRequest.newBuilder()
                        .uri(URI.create(wardUrl))
                        .header("token", apiToken)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(reqBody))
                        .build();

                HttpResponse<String> wardResponse = httpClient.send(wardRequest, HttpResponse.BodyHandlers.ofString());
                if (wardResponse.statusCode() != 200) {
                    log.warn("Failed to fetch wards for district {}. Status: {}", district.getDistrictId(), wardResponse.statusCode());
                    continue;
                }

                GhnWardApiResponse wardApiResponse = JsonHelper.deserializeSafe(wardResponse.body(), GhnWardApiResponse.class);
                if (wardApiResponse != null && wardApiResponse.data() != null) {
                    for (GhnWardRaw raw : wardApiResponse.data()) {
                        allWards.add(GhnWard.builder()
                                .wardCode(raw.WardCode())
                                .district(district)
                                .wardName(raw.WardName())
                                .build());
                    }
                }

                districtCounter++;
                if (districtCounter % 50 == 0) {
                    log.info("Fetched wards for {} / {} districts", districtCounter, allDistricts.size());
                    // Batch save wards to prevent out-of-memory or huge transaction size
                    wardRepository.saveAll(allWards);
                    allWards.clear();
                }

                // Small delay to prevent rate limit
                Thread.sleep(50);
            } catch (Exception e) {
                log.error("Error fetching wards for district ID: " + district.getDistrictId(), e);
            }
        }

        // Save remaining wards
        if (!allWards.isEmpty()) {
            wardRepository.saveAll(allWards);
        }
        log.info("Saved all wards successfully!");
    }

    // JSON Model Records for deserialization
    public record GhnProvinceRaw(
            @JsonProperty("ProvinceID") Integer ProvinceID,
            @JsonProperty("ProvinceName") String ProvinceName,
            @JsonProperty("Code") String Code
    ) {}

    public record GhnProvinceApiResponse(
            int code,
            String message,
            List<GhnProvinceRaw> data
    ) {}

    public record GhnDistrictRaw(
            @JsonProperty("DistrictID") Integer DistrictID,
            @JsonProperty("ProvinceID") Integer ProvinceID,
            @JsonProperty("DistrictName") String DistrictName,
            @JsonProperty("Code") String Code,
            @JsonProperty("SupportType") Integer SupportType
    ) {}

    public record GhnDistrictApiResponse(
            int code,
            String message,
            List<GhnDistrictRaw> data
    ) {}

    public record GhnWardRaw(
            @JsonProperty("WardCode") String WardCode,
            @JsonProperty("DistrictID") Integer DistrictID,
            @JsonProperty("WardName") String WardName
    ) {}

    public record GhnWardApiResponse(
            int code,
            String message,
            List<GhnWardRaw> data
    ) {}
}
