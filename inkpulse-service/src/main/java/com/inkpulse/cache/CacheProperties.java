package com.inkpulse.cache;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "cache")
@Getter
@Setter
public class CacheProperties {

    private Map<String, SectionConfig> sections;

    @Getter
    @Setter
    public static class SectionConfig {
        private String key;
        private int ttl;
    }

    public String buildKey(String sectionKey, String id) {
        SectionConfig config = sections.get(sectionKey);
        if (config == null) {
            throw new IllegalStateException("Cache section '" + sectionKey + "' is not configured");
        }
        return config.getKey().replace("{id}", id);
    }
}
