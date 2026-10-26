package com.ban.vehicle_management.infrastructure.security.guestsupport;

import com.ban.vehicle_management.application.operations.guestsupport.config.GuestSupportProperties;
import com.ban.vehicle_management.application.operations.guestsupport.port.out.CaptchaProofStorePortOut;
import com.ban.vehicle_management.infrastructure.cache.RedisProperties;
import com.ban.vehicle_management.shared.exception.ServiceUnavailableException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisCaptchaProofStoreAdapter implements CaptchaProofStorePortOut {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisCaptchaProofStoreAdapter.class);
    private static final DefaultRedisScript<Long> CONSUME_SCRIPT = new DefaultRedisScript<>("""
            local value = redis.call('GET', KEYS[1])
            if value == ARGV[1] then
                redis.call('DEL', KEYS[1])
                return 1
            end
            return 0
            """, Long.class);

    private final RedisProperties redisProperties;
    private final GuestSupportProperties guestProperties;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final Map<String, LocalValue> localValues = new ConcurrentHashMap<>();

    public RedisCaptchaProofStoreAdapter(
            RedisProperties redisProperties,
            GuestSupportProperties guestProperties,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider
    ) {
        this.redisProperties = redisProperties;
        this.guestProperties = guestProperties;
        this.redisTemplateProvider = redisTemplateProvider;
    }

    @Override
    public boolean claimChallengeToken(String challengeTokenHash, Duration ttl) {
        String key = key("captcha-token", challengeTokenHash);
        StringRedisTemplate redis = redisOrNull();
        if (redis != null) {
            try {
                return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, "used", ttl));
            } catch (RuntimeException exception) {
                return failOrFallback("claim", exception, () -> putIfAbsentLocal(key, "used", ttl));
            }
        }
        return failOrFallback("claim", null, () -> putIfAbsentLocal(key, "used", ttl));
    }

    @Override
    public void storeProof(String proofHash, String bindingHash, Duration ttl) {
        String key = key("captcha-proof", proofHash);
        StringRedisTemplate redis = redisOrNull();
        if (redis != null) {
            try {
                redis.opsForValue().set(key, bindingHash, ttl);
                return;
            } catch (RuntimeException exception) {
                failOrFallback("store", exception, () -> {
                    putLocal(key, bindingHash, ttl);
                    return true;
                });
                return;
            }
        }
        failOrFallback("store", null, () -> {
            putLocal(key, bindingHash, ttl);
            return true;
        });
    }

    @Override
    public boolean consumeProof(String proofHash, String bindingHash) {
        String key = key("captcha-proof", proofHash);
        StringRedisTemplate redis = redisOrNull();
        if (redis != null) {
            try {
                Long result = redis.execute(CONSUME_SCRIPT, List.of(key), bindingHash);
                return Long.valueOf(1L).equals(result);
            } catch (RuntimeException exception) {
                return failOrFallback("consume", exception, () -> consumeLocal(key, bindingHash));
            }
        }
        return failOrFallback("consume", null, () -> consumeLocal(key, bindingHash));
    }

    private StringRedisTemplate redisOrNull() {
        return redisProperties.isEnabled() ? redisTemplateProvider.getIfAvailable() : null;
    }

    private boolean failOrFallback(String operation, RuntimeException cause, java.util.function.BooleanSupplier fallback) {
        LOGGER.warn("CAPTCHA proof Redis {} unavailable", operation);
        if (!guestProperties.isRateLimitLocalFallbackEnabled()) {
            throw new ServiceUnavailableException("Dịch vụ xác minh CAPTCHA đang tạm thời không khả dụng");
        }
        return fallback.getAsBoolean();
    }

    private boolean putIfAbsentLocal(String key, String value, Duration ttl) {
        purgeExpiredLocal();
        LocalValue candidate = new LocalValue(value, Instant.now().plus(ttl));
        return localValues.compute(key, (ignored, existing) ->
                existing == null || !existing.expiresAt().isAfter(Instant.now()) ? candidate : existing) == candidate;
    }

    private void putLocal(String key, String value, Duration ttl) {
        purgeExpiredLocal();
        localValues.put(key, new LocalValue(value, Instant.now().plus(ttl)));
    }

    private boolean consumeLocal(String key, String expected) {
        LocalValue value = localValues.get(key);
        if (value == null || !value.expiresAt().isAfter(Instant.now()) || !value.value().equals(expected)) {
            return false;
        }
        return localValues.remove(key, value);
    }

    private void purgeExpiredLocal() {
        if (localValues.size() < 1_000) return;
        Instant now = Instant.now();
        localValues.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    private String key(String namespace, String digest) {
        if (digest == null || !digest.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("CAPTCHA storage key requires a SHA-256 digest");
        }
        return "vm:%s:%s:v1:public-support:%s:%s".formatted(
                safe(redisProperties.getEnvironment()), safe(redisProperties.getService()), namespace, digest);
    }

    private String safe(String value) {
        String normalized = value == null ? "unknown" : value.replaceAll("[^A-Za-z0-9._-]", "-");
        return normalized.isBlank() ? "unknown" : normalized;
    }

    private record LocalValue(String value, Instant expiresAt) {
    }
}
