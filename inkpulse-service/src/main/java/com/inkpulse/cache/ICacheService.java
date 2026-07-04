package com.inkpulse.cache;

import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface ICacheService {

    // ─── String ───────────────────────────────────────────────────────

    void setString(String key, String value, Duration expiry);

    String getString(String key);

    // ─── Typed Object (JSON-serialized) ───────────────────────────────

    <T> void set(String key, T value, Duration expiry);

    <T> T get(String key, Class<T> type);

    <T> T getOrSet(String key, Supplier<T> factory, Duration expiry, Class<T> type);

    // ─── Key ──────────────────────────────────────────────────────────

    void remove(String key);

    void removeByPrefix(String prefix);

    // ─── Distributed Lock ─────────────────────────────────────────────

    boolean acquireLock(String lockKey, String lockValue, Duration expiry,
            boolean retry, Duration retryTimeout, Duration retryInterval);

    boolean releaseLock(String lockKey, String lockValue);

    // ─── Pub/Sub ──────────────────────────────────────────────────────

    void publish(String channel, String message);

    void subscribe(String channel, Consumer<String> handler);

    // ─── Hash ─────────────────────────────────────────────────────────

    void hashSet(String key, Map<String, String> fields, Duration expiry);

    Map<String, String> hashGetAll(String key);

    String hashGet(String key, String field);

    void hashDelete(String key, String field);

    // ─── Set ─────────────────────────────────────────────────────────

    void sadd(String key, Duration ttl, String... members);

    java.util.Set<String> smembers(String key);

    void srem(String key, String... members);
}
