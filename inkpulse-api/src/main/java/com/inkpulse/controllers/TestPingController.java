package com.inkpulse.controllers;

import com.inkpulse.models.response.ResultRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TestPingController {

    @GetMapping("/api/v1/public/ping-test")
    public ResponseEntity<ResultRes<Map<String, String>>> pingTest() {
        log.info("REST request to ping test endpoint");
        return ResponseEntity.ok(ResultRes.successResult(
                Map.of("status", "UP", "message", "CI/CD Deployment Verified Successfully"),
                "Ping test successful",
                200
        ));
    }
}
