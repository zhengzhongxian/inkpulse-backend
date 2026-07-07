package com.inkpulse.config;

import com.inkpulse.constants.KeyConstants;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.inkpulse.service.payos.PayOsSettings;

@Configuration
@ConfigurationProperties(prefix = KeyConstants.PAYOS_PREFIX)
@Getter
@Setter
public class PayOsProperties implements PayOsSettings {
    private String clientId;
    private String apiKey;
    private String checksumKey;
    private String returnUrl;
    private String cancelUrl;
    private int expiryMinutes = 15;
}
