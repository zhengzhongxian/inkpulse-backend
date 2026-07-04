package com.inkpulse.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class SectionCacheService {

    private final ICacheService cacheService;
    private final CacheProperties cacheProperties;

    private final ConcurrentHashMap<Class<?>, CacheProperties.SectionConfig> configCache = new ConcurrentHashMap<>();

    public void set(Cacheable value) {
        CacheProperties.SectionConfig config = getSectionConfig(value.getClass());
        String finalKey = config.getKey() + value.cacheId();
        cacheService.set(finalKey, value, Duration.ofMinutes(config.getTtl()));
    }

    public <T extends Cacheable> T get(String identifier, Class<T> clazz) {
        CacheProperties.SectionConfig config = getSectionConfig(clazz);
        String finalKey = config.getKey() + identifier;
        return cacheService.get(finalKey, clazz);
    }

    public void remove(String identifier, Class<? extends Cacheable> clazz) {
        CacheProperties.SectionConfig config = getSectionConfig(clazz);
        String finalKey = config.getKey() + identifier;
        cacheService.remove(finalKey);
    }

    public boolean exists(String identifier, Class<? extends Cacheable> clazz) {
        return get(identifier, clazz) != null;
    }

    private CacheProperties.SectionConfig getSectionConfig(Class<?> clazz) {
        return configCache.computeIfAbsent(clazz, clz -> {
            CacheSection annotation = clz.getAnnotation(CacheSection.class);
            if (annotation == null) {
                throw new IllegalArgumentException(
                        "Class " + clz.getSimpleName() + " is missing @CacheSection annotation. " +
                                "Add @CacheSection(\"section-name\") to the class.");
            }
            CacheProperties.SectionConfig config = cacheProperties.getSections().get(annotation.value());
            if (config == null) {
                throw new IllegalArgumentException(
                        "Cache section '" + annotation.value() + "' is not configured. " +
                                "Add it to cache.sections in application.yml.");
            }
            return config;
        });
    }
}
