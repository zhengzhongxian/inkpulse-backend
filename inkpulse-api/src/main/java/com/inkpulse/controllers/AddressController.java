package com.inkpulse.controllers;

import an.awesome.pipelinr.Pipeline;
import com.inkpulse.features.address.queries.GetGhnDistrictsQuery;
import com.inkpulse.features.address.queries.GetGhnProvincesQuery;
import com.inkpulse.features.address.queries.GetGhnWardsQuery;
import com.inkpulse.models.response.GhnDistrictResponse;
import com.inkpulse.models.response.GhnProvinceResponse;
import com.inkpulse.models.response.GhnWardResponse;
import com.inkpulse.models.response.ResultRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.inkpulse.constants.message.AddressMessageConstants;
import java.util.List;

@RestController
@RequestMapping("/api/v1/public/address")
@RequiredArgsConstructor
@Slf4j
public class AddressController {

    private final Pipeline pipeline;

    @GetMapping("/provinces")
    public ResponseEntity<ResultRes<List<GhnProvinceResponse>>> getProvinces() {
        log.info("REST request to list provinces");
        List<GhnProvinceResponse> result = pipeline.send(new GetGhnProvincesQuery());
        return ResponseEntity.ok(ResultRes.successResult(result, AddressMessageConstants.GET_PROVINCES_SUCCESS, 200));
    }

    @GetMapping("/districts")
    public ResponseEntity<ResultRes<List<GhnDistrictResponse>>> getDistricts(@RequestParam Integer provinceId) {
        log.info("REST request to list districts for provinceId: {}", provinceId);
        List<GhnDistrictResponse> result = pipeline.send(new GetGhnDistrictsQuery(provinceId));
        return ResponseEntity.ok(ResultRes.successResult(result, AddressMessageConstants.GET_DISTRICTS_SUCCESS, 200));
    }

    @GetMapping("/wards")
    public ResponseEntity<ResultRes<List<GhnWardResponse>>> getWards(@RequestParam Integer districtId) {
        log.info("REST request to list wards for districtId: {}", districtId);
        List<GhnWardResponse> result = pipeline.send(new GetGhnWardsQuery(districtId));
        return ResponseEntity.ok(ResultRes.successResult(result, AddressMessageConstants.GET_WARDS_SUCCESS, 200));
    }
}
