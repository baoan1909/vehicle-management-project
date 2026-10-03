package com.ban.vehicle_management.infrastructure.security.ratelimit;

import com.ban.vehicle_management.infrastructure.cache.RedisProperties;
import com.ban.vehicle_management.shared.exception.MapRequestLimitException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class MapApiRateLimiter {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DefaultRedisScript<Long> INCREMENT_WINDOW_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]))
            end
            return current
            """, Long.class);

    private final MapApiRateLimitProperties properties;
    private final RedisProperties redisProperties;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final Map<String, AtomicInteger> localWindows = new ConcurrentHashMap<>();

    public MapApiRateLimiter(
            MapApiRateLimitProperties properties,
            RedisProperties redisProperties,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider
    ) {
        this.properties = properties;
        this.redisProperties = redisProperties;
        this.redisTemplateProvider = redisTemplateProvider;
    }

    public void check(String remoteAddress, UUID accountId) {
        if (!properties.isEnabled()) return;
        long windowSeconds = properties.getWindow().toSeconds();
        long windowNumber = Math.floorDiv(Instant.now().getEpochSecond(), windowSeconds);
        OffsetDateTime retryAfter = Instant.ofEpochSecond((windowNumber + 1L) * windowSeconds)
                .atZone(VIETNAM_ZONE)
                .toOffsetDateTime();

        String ipIdentity = hashIdentity(remoteAddress == null ? "unknown" : remoteAddress);
        int ipLimit = accountId == null
                ? properties.getAnonymousIpLimit()
                : properties.getAuthenticatedIpLimit();
        enforce("ip", ipIdentity, windowNumber, windowSeconds, ipLimit, retryAfter);
        if (accountId != null) {
            enforce("account", accountId.toString(), windowNumber, windowSeconds,
                    properties.getAccountLimit(), retryAfter);
        }
    }

    private void enforce(
            String dimension,
            String identity,
            long windowNumber,
            long windowSeconds,
            int limit,
            OffsetDateTime retryAfter
    ) {
        String key = "maps:ratelimit:%s:%s:%d".formatted(dimension, identity, windowNumber);
        long count = increment(key, windowSeconds);
        if (count > limit) {
            throw new MapRequestLimitException(
                    "MAP_RATE_LIMIT_EXCEEDED",
                    "Bạn gửi quá nhiều yêu cầu tìm kiếm bản đồ. Vui lòng thử lại sau.",
                    retryAfter
            );
        }
    }

    private long increment(String key, long windowSeconds) {
        if (redisProperties.isEnabled()) {
            try {
                StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
                if (redisTemplate != null) {
                    Long result = redisTemplate.execute(
                            INCREMENT_WINDOW_SCRIPT,
                            List.of(key),
                            String.valueOf(Math.max(1L, windowSeconds + 1L))
                    );
                    if (result != null) return result;
                }
            } catch (RuntimeException ignored) {
                // Local fallback still protects this instance without exposing client identity.
            }
        }
        if (localWindows.size() > 10_000) {
            long activeWindow = Long.parseLong(key.substring(key.lastIndexOf(':') + 1));
            localWindows.keySet().removeIf(candidate -> !candidate.endsWith(':' + String.valueOf(activeWindow)));
        }
        return localWindows.computeIfAbsent(key, ignored -> new AtomicInteger()).incrementAndGet();
    }

    private String hashIdentity(String identity) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(identity.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 16);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
