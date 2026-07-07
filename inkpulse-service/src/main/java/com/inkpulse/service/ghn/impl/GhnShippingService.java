package com.inkpulse.service.ghn.impl;

import com.inkpulse.models.request.ghn.GhnCalculateFeeRequest;
import com.inkpulse.models.response.ghn.GhnCalculateFeeResponse;
import com.inkpulse.service.ghn.GhnSettings;
import com.inkpulse.service.ghn.IGhnShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class GhnShippingService implements IGhnShippingService {

    private final GhnSettings ghnSettings;
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public GhnCalculateFeeResponse calculateShippingFee(GhnCalculateFeeRequest request) {
        log.info("Calculating shipping fee to district: {}, ward: {}", request.getToDistrictId(), request.getToWardCode());
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Token", ghnSettings.getApiToken());
            headers.set("ShopId", String.valueOf(ghnSettings.getShopId()));

            HttpEntity<GhnCalculateFeeRequest> entity = new HttpEntity<>(request, headers);
            String url = ghnSettings.getBaseUrl() + "/shiip/public-api/v2/shipping-order/fee";

            ResponseEntity<GhnCalculateFeeResponse> response = restTemplate.postForEntity(url, entity, GhnCalculateFeeResponse.class);
            if (response.getBody() == null || response.getBody().getCode() != 200) {
                String errMsg = response.getBody() != null ? response.getBody().getMessage() : "Unknown GHN error";
                log.error("GHN fee calculation failed: {}", errMsg);
                throw new RuntimeException("GHN fee calculation failed: " + errMsg);
            }

            return response.getBody();
        } catch (Exception e) {
            log.error("Exception calling GHN fee calculation API", e);
            throw new RuntimeException("Failed to calculate shipping fee: " + e.getMessage(), e);
        }
    }
}
