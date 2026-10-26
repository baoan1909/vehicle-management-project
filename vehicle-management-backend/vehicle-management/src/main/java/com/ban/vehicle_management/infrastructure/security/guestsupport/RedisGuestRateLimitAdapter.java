package com.ban.vehicle_management.infrastructure.security.guestsupport;

import com.ban.vehicle_management.application.operations.guestsupport.config.GuestSupportProperties;
import com.ban.vehicle_management.application.operations.guestsupport.model.GuestRateLimitBucket;
import com.ban.vehicle_management.application.operations.guestsupport.model.GuestRateLimitDecision;
import com.ban.vehicle_management.application.operations.guestsupport.port.out.GuestRateLimitPortOut;
import com.ban.vehicle_management.infrastructure.cache.RedisProperties;
import com.ban.vehicle_management.shared.exception.ServiceUnavailableException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisGuestRateLimitAdapter implements GuestRateLimitPortOut {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisGuestRateLimitAdapter.class);
    private static final int MAX_LOCAL_KEYS = 10_000;
    private static final DefaultRedisScript<String> CONSUME_SCRIPT = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
                redis.call('PEXPIRE', KEYS[1], tonumber(ARGV[1]))
            end
            return tostring(count) .. ':' .. tostring(redis.call('PTTL', KEYS[1]))
            """, String.class);

    private final RedisProperties redisProperties;
    private final GuestSupportProperties guestProperties;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final GuestSupportSecurityMetrics metrics;
    private final Map<String, LocalWindow> localWindows = new ConcurrentHashMap<>();
    private final AtomicLong localRequests = new AtomicLong();

    public RedisGuestRateLimitAdapter(
            RedisProperties redisProperties,
            GuestSupportProperties guestProperties,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            GuestSupportSecurityMetrics metrics
    ) {
        this.redisProperties = redisProperties;
        this.guestProperties = guestProperties;
        this.redisTemplateProvider = redisTemplateProvider;
        this.metrics = metrics;
    }

    @Override
    public GuestRateLimitDecision consume(
            GuestRateLimitBucket bucket,
            String hashedIdentity,
            int limit,
            Duration window
    ) {
        if (limit <= 0 || window == null || window.isNegative() || window.isZero()) {
            throw new IllegalStateException("Guest rate-limit configuration is invalid");
        }
        String key = key(bucket, hashedIdentity);
        long windowMillis = Math.max(1L, window.toMillis());
        if (redisProperties.isEnabled()) {
            try {
                StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
                if (redis != null) {
                    String raw = redis.execute(CONSUME_SCRIPT, List.of(key), String.valueOf(windowMillis));
                    GuestRateLimitDecision decision = parse(raw, limit, windowMillis);
                    metrics.rateLimit(bucket, decision.allowed() ? "allowed" : "limited");
                    return decision;
                }
            } catch (RuntimeException exception) {
                LOGGER.warn("Guest public-support Redis rate limiter unavailable for bucket {}", bucket);
            }
        }
        metrics.rateLimit(bucket, "redis_unavailable");
        if (!guestProperties.isRateLimitLocalFallbackEnabled()) {
            throw new ServiceUnavailableException("Bảo vệ chống lạm dụng đang tạm thời không khả dụng");
        }
        GuestRateLimitDecision decision = consumeLocal(key, limit, window);
        metrics.rateLimit(bucket, decision.allowed() ? "allowed_fallback" : "limited_fallback");
        return decision;
    }

    private GuestRateLimitDecision parse(String raw, int limit, long windowMillis) {
        if (raw == null || !raw.matches("[0-9]+:-?[0-9]+")) {
            throw new IllegalStateException("Redis returned an invalid rate-limit decision");
        }
        String[] parts = raw.split(":", 2);
        long count = Long.parseLong(parts[0]);
        long ttlMillis = Math.max(1L, Long.parseLong(parts[1]));
        return new GuestRateLimitDecision(count <= limit, Math.max(1L, (ttlMillis + 999L) / 1000L));
    }

    private GuestRateLimitDecision consumeLocal(String key, int limit, Duration window) {
        Instant now = Instant.now();
        if ((localRequests.incrementAndGet() & 255L) == 0L) {
            localWindows.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        }
        if (!localWindows.containsKey(key) && localWindows.size() >= MAX_LOCAL_KEYS) {
            return new GuestRateLimitDecision(false, Math.max(1L, window.toSeconds()));
        }
        LocalWindow current = localWindows.compute(key, (ignored, previous) -> {
            if (previous == null || !previous.expiresAt().isAfter(now)) {
                return new LocalWindow(1, now.plus(window));
            }
            return new LocalWindow(previous.count() + 1, previous.expiresAt());
        });
        long retry = Math.max(1L, Duration.between(now, current.expiresAt()).toSeconds());
        return new GuestRateLimitDecision(current.count() <= limit, retry);
    }

    private String key(GuestRateLimitBucket bucket, String hashedIdentity) {
        if (hashedIdentity == null || !hashedIdentity.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("Rate-limit identity must be a SHA-256 digest");
        }
        return "vm:%s:%s:v1:public-support-rate-limit:%s:%s".formatted(
                safe(redisProperties.getEnvironment()), safe(redisProperties.getService()),
                bucket.keySegment(), hashedIdentity);
    }

    private String safe(String value) {
        String normalized = value == null ? "unknown" : value.replaceAll("[^A-Za-z0-9._-]", "-");
        return normalized.isBlank() ? "unknown" : normalized;
    }

    private record LocalWindow(long count, Instant expiresAt) {
    }
}
