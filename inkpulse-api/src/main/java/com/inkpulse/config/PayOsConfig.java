package com.inkpulse.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

@Configuration
public class PayOsConfig {

    @Bean
    public PayOS payOS(PayOsProperties payOsProperties) {
        return new PayOS(
            payOsProperties.getClientId(),
            payOsProperties.getApiKey(),
            payOsProperties.getChecksumKey()
        );
    }
}
