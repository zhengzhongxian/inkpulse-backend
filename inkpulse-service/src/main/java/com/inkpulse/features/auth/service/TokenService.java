package com.inkpulse.features.auth.service;

import com.inkpulse.constants.KeyConstants;
import com.inkpulse.cache.ICacheService;
import com.inkpulse.cache.SectionCacheService;
import com.github.f4b6a3.uuid.UuidCreator;
import com.inkpulse.entities.*;
import com.inkpulse.entities.enums.DisplayMode;
import com.inkpulse.entities.enums.Language;
import com.inkpulse.features.auth.dto.BlacklistedTokenDto;
import com.inkpulse.features.auth.dto.RefreshTokenDto;
import com.inkpulse.features.auth.dto.UserSessionDto;
import com.inkpulse.repositories.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    private final SectionCacheService sectionCache;
    private final ICacheService cacheService;
    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final UserSettingRepository userSettingRepository;
    private final CartRepository cartRepository;

    @Value("${" + KeyConstants.JWT_SECRET + "}")
    private String jwtSecret;

    @Value("${" + KeyConstants.JWT_ACCESS_TOKEN_TTL + "}")
    private int accessTokenTtlMinutes;

    @Value("${" + KeyConstants.JWT_REFRESH_TOKEN_TTL + "}")
    private int refreshTokenTtlMinutes;

    private static final RedisScript<List> REFRESH_TOKEN_SCRIPT = RedisScript.of(
            "local tokenKey = KEYS[1] " +
                    "local exists = redis.call('exists', tokenKey) " +
                    "if exists == 0 then " +
                    "    return {'NOT_FOUND'} " +
                    "end " +
                    "local is_revoked = redis.call('hget', tokenKey, 'is_revoked') " +
                    "local userId = redis.call('hget', tokenKey, 'user_id') " +
                    "local deviceId = redis.call('hget', tokenKey, 'device_id') " +
                    "local oldTokenId = redis.call('hget', tokenKey, 'token_id') " +
                    "if is_revoked == 'true' then " +
                    "    local keys = redis.call('keys', 'rt:*') " +
                    "    for _, k in ipairs(keys) do " +
                    "        local u = redis.call('hget', k, 'user_id') " +
                    "        local d = redis.call('hget', k, 'device_id') " +
                    "        if u == userId and d == deviceId then " +
                    "            redis.call('del', k) " +
                    "        end " +
                    "    end " +
                    "    return {'BREACH', userId, deviceId} " +
                    "else " +
                    "    redis.call('hset', tokenKey, 'is_revoked', 'true') " +
                    "    return {'SUCCESS', userId, deviceId, oldTokenId} " +
                    "end",
            List.class);

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        // noinspection SpellCheckingInspection
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    public String generateAccessToken(UUID userId, String username) {
        String jti = UuidCreator.getTimeOrderedEpoch().toString();
        return generateAccessToken(userId, username, jti);
    }

    public String generateAccessToken(UUID userId, String username, String jti) {
        List<String> roles = collectRolesFromDb(userId);
        return generateAccessToken(userId, username, roles, jti);
    }

    public String generateAccessToken(UUID userId, String username, List<String> roles) {
        String jti = UuidCreator.getTimeOrderedEpoch().toString();
        return generateAccessToken(userId, username, roles, jti);
    }

    public String generateAccessToken(UUID userId, String username, List<String> roles, String jti) {
        Instant now = Instant.now();
        Instant expiry = now.plus(Duration.ofMinutes(accessTokenTtlMinutes));

        return Jwts.builder()
                .subject(userId.toString())
                .claim("username", username)
                .claim("roles", roles)
                .id(jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims parseAccessToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID getUserIdFromToken(String token) {
        Claims claims = parseAccessToken(token);
        return UUID.fromString(claims.getSubject());
    }

    public String generateRefreshToken(UUID userId, UUID deviceId) {
        String tokenJti = UUID.randomUUID().toString();
        return generateRefreshToken(userId, deviceId, tokenJti, null);
    }

    public String generateRefreshToken(UUID userId, UUID deviceId, String tokenJti, String parentTokenId) {
        String rawToken = UUID.randomUUID() + "-" + UUID.randomUUID();
        String tokenHash = sha256(rawToken);
        String key = "rt:" + tokenHash;

        Map<String, String> fields = new HashMap<>();
        fields.put("user_id", userId.toString());
        fields.put("device_id", deviceId.toString());
        fields.put("is_revoked", "false");
        fields.put("token_id", tokenJti);
        fields.put("parent_token_id", parentTokenId != null ? parentTokenId : "");

        cacheService.hashSet(key, fields, Duration.ofMinutes(refreshTokenTtlMinutes));

        return rawToken;
    }

    public RotationResult validateAndRotateRefreshToken(String rawToken) {
        String tokenHash = sha256(rawToken);
        String key = "rt:" + tokenHash;

        List<String> results = redisTemplate.execute(REFRESH_TOKEN_SCRIPT, List.of(key));

        if (results == null || results.isEmpty() || "NOT_FOUND".equals(results.get(0))) {
            return new RotationResult(RotationStatus.NOT_FOUND, null, null, null);
        }

        UUID userId = UUID.fromString(results.get(1));
        UUID deviceId = UUID.fromString(results.get(2));

        if ("BREACH".equals(results.get(0))) {
            log.error("Refresh token reuse breach detected! Revoked all tokens for userId: {}, deviceId: {}", userId,
                    deviceId);
            return new RotationResult(RotationStatus.BREACH, userId, deviceId, null);
        }

        String oldTokenId = results.get(3);
        return new RotationResult(RotationStatus.SUCCESS, userId, deviceId, oldTokenId);
    }

    public void revokeAllUserRefreshTokens(UUID userId) {
        log.info("Revoking all refresh tokens for user: {}", userId);
        try {
            Set<String> keys = redisTemplate.keys("rt:*");
            if (keys != null) {
                for (String key : keys) {
                    String uId = (String) redisTemplate.opsForHash().get(key, "user_id");
                    if (userId.toString().equals(uId)) {
                        redisTemplate.delete(key);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to revoke refresh tokens for user: {}", userId, e);
        }
    }

    public void revokeRefreshToken(String rawToken) {
        try {
            String tokenHash = sha256(rawToken);
            String key = "rt:" + tokenHash;
            redisTemplate.delete(key);
            log.info("Revoked specific refresh token: {}", key);
        } catch (Exception e) {
            log.error("Failed to revoke specific refresh token: {}", rawToken, e);
        }
    }

    public UUID getUserIdFromRefreshToken(String rawToken) {
        String tokenHash = sha256(rawToken);
        String key = "rt:" + tokenHash;
        String userIdStr = cacheService.hashGet(key, "user_id");
        return userIdStr != null ? UUID.fromString(userIdStr) : null;
    }

    public void storeUserSession(UUID userId, List<String> roles, List<String> permissions,
            DisplayMode displayMode, Language language) {
        UserSessionDto sessionDto = new UserSessionDto(
                userId.toString(),
                roles,
                permissions,
                displayMode != null ? displayMode.name() : "SYSTEM",
                language != null ? language.name() : "VI");
        sectionCache.set(sessionDto);
    }

    public List<String> collectRolesFromDb(UUID userId) {
        return userRoleRepository.findAllByUserId(userId).stream()
                .map(ur -> ur.getRole().getRoleCode())
                .distinct()
                .toList();
    }

    public List<String> collectPermissionsFromDb(UUID userId) {
        Set<String> permissions = new LinkedHashSet<>();

        List<UserRole> userRoles = userRoleRepository.findAllByUserId(userId);
        for (UserRole ur : userRoles) {
            List<RolePermission> rolePerms = rolePermissionRepository.findAllByRoleId(ur.getRole().getId());
            rolePerms.stream()
                    .map(rp -> rp.getPermission().getPermissionCode())
                    .forEach(permissions::add);
        }

        List<UserPermission> directPerms = userPermissionRepository.findAllByUserId(userId);
        directPerms.stream()
                .map(up -> up.getPermission().getPermissionCode())
                .forEach(permissions::add);

        return new ArrayList<>(permissions);
    }

    @Transactional(readOnly = true)
    public UserSessionDto loadAndCacheUserSession(UUID userId) {
        log.info("Cache miss for user session: {}. Loading from database...", userId);
        List<String> roles = collectRolesFromDb(userId);
        List<String> permissions = collectPermissionsFromDb(userId);
        UserSetting setting = userSettingRepository.findByUserId(userId).orElse(null);

        // Store session in cache
        storeUserSession(
                userId, roles, permissions,
                setting != null ? setting.getDisplayMode() : DisplayMode.SYSTEM,
                setting != null ? setting.getChoiceLanguage() : Language.VI);

        // Also eager cache cart items for safety
        cacheUserCart(userId);

        return getUserSession(userId);
    }

    public List<com.inkpulse.features.cart.dto.UserCartCacheDto.CartItemDto> collectCartItemsFromDb(UUID userId) {
        return cartRepository.findByUserId(userId)
                .map(Cart::getItems)
                .orElse(Collections.emptySet())
                .stream()
                .map(item -> new com.inkpulse.features.cart.dto.UserCartCacheDto.CartItemDto(
                        item.getId().toString(),
                        item.getEdition().getId().toString(),
                        item.getQuantity()))
                .toList();
    }

    public void storeUserCart(UUID userId, List<com.inkpulse.features.cart.dto.UserCartCacheDto.CartItemDto> items) {
        com.inkpulse.features.cart.dto.UserCartCacheDto cartDto = new com.inkpulse.features.cart.dto.UserCartCacheDto(
                userId.toString(),
                items);
        sectionCache.set(cartDto);
    }

    public void cacheUserCart(UUID userId) {
        List<com.inkpulse.features.cart.dto.UserCartCacheDto.CartItemDto> items = collectCartItemsFromDb(userId);
        storeUserCart(userId, items);
        log.info("Eager cached cart items for user: {}, count: {}", userId, items.size());
    }

    public UserSessionDto getUserSession(UUID userId) {
        return sectionCache.get(userId.toString(), UserSessionDto.class);
    }

    public void removeUserSession(UUID userId) {
        sectionCache.remove(userId.toString(), UserSessionDto.class);
    }

    public void removeUserCart(UUID userId) {
        sectionCache.remove(userId.toString(), com.inkpulse.features.cart.dto.UserCartCacheDto.class);
    }

    /**
     * Blacklists an access token in Redis.
     * TTL is governed by the "redis:blacklisted_tokens" section config in
     * application.yml
     * (should match access-token-ttl so the entry auto-expires when the token would
     * have expired anyway).
     *
     * @param jti        JWT ID claim (unique per token)
     * @param userId     Subject of the token
     * @param reasonCode Reason for revocation (e.g. DIRECTLY_LOGOUT,
     *                   CHANGE_PASSWORD)
     */
    public void blacklistAccessToken(String jti, String userId, String reasonCode) {
        BlacklistedTokenDto dto = new BlacklistedTokenDto(jti, userId, reasonCode);
        sectionCache.set(dto);
        log.info("Access token blacklisted: jti={}, user={}, reason={}", jti, userId, reasonCode);
    }

    /**
     * Checks whether the given JWT ID (jti) has been blacklisted.
     * Called in JwtAuthenticationFilter before authenticating the request.
     */
    public boolean isTokenBlacklisted(String jti) {
        return sectionCache.exists(jti, BlacklistedTokenDto.class);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public enum RotationStatus {
        SUCCESS, BREACH, NOT_FOUND
    }

    public record RotationResult(
            RotationStatus status,
            UUID userId,
            UUID deviceId,
            String oldTokenId) {
    }
}
