package com.inkpulse.cache.impl;

import com.inkpulse.cache.ICacheService;
import com.inkpulse.corehelpers.JsonHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheService implements ICacheService {

    private final StringRedisTemplate redisTemplate;
    private final RedisMessageListenerContainer listenerContainer;

    // Lua: atomic lock release — only deletes if value matches (owner check)
    private static final RedisScript<Long> RELEASE_LOCK_SCRIPT = RedisScript.of(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) " +
                    "else return 0 end",
            Long.class);

    // ─── String ───────────────────────────────────────────────────────

    @Override
    public void setString(String key, String value, Duration expiry) {
        redisTemplate.opsForValue().set(key, value, expiry);
    }

    @Override
    public String getString(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    @Override
    public void expire(String key, Duration expiry) {
        redisTemplate.expire(key, expiry);
    }

    // ─── Typed Object ─────────────────────────────────────────────────

    @Override
    public <T> void set(String key, T value, Duration expiry) {
        String json = JsonHelper.serializeSafe(value);
        if (json.isEmpty()) {
            log.warn("Failed to serialize value for key: {}", key);
            return;
        }
        redisTemplate.opsForValue().set(key, json, expiry);
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank())
            return null;
        return JsonHelper.deserializeSafe(json, type);
    }

    @Override
    public <T> T getOrSet(String key, Supplier<T> factory, Duration expiry, Class<T> type) {
        T cached = get(key, type);
        if (cached != null)
            return cached;

        T value = factory.get();
        if (value != null) {
            set(key, value, expiry);
        }
        return value;
    }

    // ─── Key ──────────────────────────────────────────────────────────

    @Override
    public void remove(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public void removeByPrefix(String prefix) {
        try {
            Set<String> keysToDelete = new HashSet<>();
            ScanOptions options = ScanOptions.scanOptions()
                    .match(prefix + "*")
                    .count(100)
                    .build();

            try (Cursor<String> cursor = redisTemplate.scan(options)) {
                while (cursor.hasNext()) {
                    keysToDelete.add(cursor.next());
                }
            }

            if (!keysToDelete.isEmpty()) {
                redisTemplate.delete(keysToDelete);
                log.debug("Removed {} keys with prefix: {}", keysToDelete.size(), prefix);
            }
        } catch (Exception e) {
            log.error("Failed to remove keys by prefix: {}", prefix, e);
        }
    }

    // ─── Distributed Lock ─────────────────────────────────────────────

    @Override
    public boolean acquireLock(String lockKey, String lockValue, Duration expiry,
            boolean retry, Duration retryTimeout, Duration retryInterval) {
        if (!retry) {
            Boolean result = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, expiry);
            return Boolean.TRUE.equals(result);
        }

        long deadline = System.currentTimeMillis() + retryTimeout.toMillis();

        while (System.currentTimeMillis() < deadline) {
            Boolean result = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, expiry);
            if (Boolean.TRUE.equals(result)) {
                return true;
            }
            try {
                Thread.sleep(retryInterval.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        return false;
    }

    @Override
    public boolean releaseLock(String lockKey, String lockValue) {
        Long result = redisTemplate.execute(RELEASE_LOCK_SCRIPT, List.of(lockKey), lockValue);
        return result != null && result == 1L;
    }

    // ─── Pub/Sub ──────────────────────────────────────────────────────

    @Override
    public void publish(String channel, String message) {
        redisTemplate.convertAndSend(channel, message);
    }

    @Override
    public void subscribe(String channel, Consumer<String> handler) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(
                (MessageListener) (message, pattern) -> {
                    String body = new String(message.getBody(), StandardCharsets.UTF_8);
                    try {
                        handler.accept(body);
                    } catch (Exception e) {
                        log.error("Error processing message on channel {}: {}",
                                channel, e.getMessage(), e);
                    }
                });

        listenerContainer.addMessageListener(adapter, new ChannelTopic(channel));
    }

    // ─── Hash ─────────────────────────────────────────────────────────

    @Override
    public void hashSet(String key, Map<String, String> fields, Duration expiry) {
        redisTemplate.opsForHash().putAll(key, fields);
        if (expiry != null) {
            redisTemplate.expire(key, expiry);
        }
    }

    @Override
    public Map<String, String> hashGetAll(String key) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        Map<String, String> result = new LinkedHashMap<>();
        entries.forEach((k, v) -> result.put(k.toString(), v.toString()));
        return result;
    }

    @Override
    public String hashGet(String key, String field) {
        Object value = redisTemplate.opsForHash().get(key, field);
        return value != null ? value.toString() : null;
    }

    @Override
    public void hashDelete(String key, String field) {
        redisTemplate.opsForHash().delete(key, field);
    }

    @Override
    public Long hashIncrement(String key, String field, long delta) {
        return redisTemplate.opsForHash().increment(key, field, delta);
    }

    // ─── Set ─────────────────────────────────────────────────────────

    @Override
    public void sadd(String key, Duration ttl, String... members) {
        if (members == null || members.length == 0)
            return;
        redisTemplate.opsForSet().add(key, members);
        if (ttl != null) {
            redisTemplate.expire(key, ttl);
        }
    }

    @Override
    public java.util.Set<String> smembers(String key) {
        java.util.Set<String> result = new java.util.HashSet<>();
        Set<String> members = redisTemplate.opsForSet().members(key);
        if (members != null) {
            result.addAll(members);
        }
        return result;
    }

    @Override
    public void srem(String key, String... members) {
        if (members == null || members.length == 0)
            return;
        redisTemplate.opsForSet().remove(key, (Object[]) members);
    }

    @Override
    public boolean sismember(String key, String member) {
        Boolean isMember = redisTemplate.opsForSet().isMember(key, member);
        return isMember != null && isMember;
    }
}
