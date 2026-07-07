package com.inkpulse.config;

import com.inkpulse.constants.KeyConstants;
import com.inkpulse.service.ghn.GhnSettings;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = KeyConstants.GHN_PREFIX)
@Getter
@Setter
public class GhnProperties implements GhnSettings {
    private String apiToken;
    private String baseUrl;
    private int shopId;
}
