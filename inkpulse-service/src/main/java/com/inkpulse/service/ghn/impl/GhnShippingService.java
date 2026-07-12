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

import java.util.List;
import java.util.Map;

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

    @Override
    public String generatePrintToken(String ghnOrderCode) {
        log.info("Generating print token from GHN for code: {}", ghnOrderCode);
        
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Token", ghnSettings.getApiToken());
            headers.set("ShopId", String.valueOf(ghnSettings.getShopId()));

            Map<String, Object> body = Map.of("order_codes", List.of(ghnOrderCode));
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String url = ghnSettings.getBaseUrl() + "/shiip/public-api/v2/a5/gen-token";

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getBody() == null || !Integer.valueOf(200).equals(response.getBody().get("code"))) {
                String errMsg = response.getBody() != null ? String.valueOf(response.getBody().get("message")) : "Unknown GHN error";
                log.error("GHN token generation failed: {}", errMsg);
                throw new RuntimeException("GHN token generation failed: " + errMsg);
            }

            Map data = (Map) response.getBody().get("data");
            if (data == null || data.get("token") == null) {
                log.error("GHN response data token is missing");
                throw new RuntimeException("Token missing from GHN response data");
            }

            return String.valueOf(data.get("token"));
        } catch (Exception e) {
            log.error("Exception calling GHN gen-token API", e);
            throw new RuntimeException("Failed to generate print token: " + e.getMessage(), e);
        }
    }

    @Override
    public void updateShippingOrder(String ghnOrderCode, String note, String requiredNote, Integer weight, Integer length, Integer width, Integer height) {
        log.info("Updating shipping order in GHN for code: {}", ghnOrderCode);
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Token", ghnSettings.getApiToken());
            headers.set("ShopId", String.valueOf(ghnSettings.getShopId()));

            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("order_code", ghnOrderCode);
            if (note != null) body.put("note", note);
            if (requiredNote != null) body.put("required_note", requiredNote);
            if (weight != null) body.put("weight", weight);
            if (length != null) body.put("length", length);
            if (width != null) body.put("width", width);
            if (height != null) body.put("height", height);

            HttpEntity<java.util.Map<String, Object>> entity = new HttpEntity<>(body, headers);
            String url = ghnSettings.getBaseUrl() + "/shiip/public-api/v2/shipping-order/update";

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            if (response.getBody() == null || !Integer.valueOf(200).equals(response.getBody().get("code"))) {
                String errMsg = response.getBody() != null ? String.valueOf(response.getBody().get("message")) : "Unknown GHN error";
                log.error("GHN shipping order update failed: {}", errMsg);
                throw new RuntimeException("GHN update failed: " + errMsg);
            }
            log.info("Successfully updated GHN shipping order: {}", ghnOrderCode);
        } catch (Exception e) {
            log.error("Exception calling GHN update API", e);
            throw new RuntimeException("Failed to update shipping order: " + e.getMessage(), e);
        }
    }
}
