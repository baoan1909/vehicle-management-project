package com.ban.vehicle_management.infrastructure.cache;

import com.ban.vehicle_management.application.ai.port.out.DistributedCachePortOut;
import com.ban.vehicle_management.application.ai.service.AiCacheProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Fail-open Redis cache adapter. Every Redis/timeout/deserialization problem is
 * swallowed, metered and surfaced as a miss so chat behaviour never changes.
 */
@Component
public class RedisDistributedCacheAdapter implements DistributedCachePortOut {

    private static final Logger log = LoggerFactory.getLogger(RedisDistributedCacheAdapter.class);

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final RedisProperties redisProperties;
    private final AiCacheProperties cacheProperties;
    private final ObjectMapper objectMapper;
    private final AiCacheMetrics metrics;

    public RedisDistributedCacheAdapter(
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            RedisProperties redisProperties,
            AiCacheProperties cacheProperties,
            ObjectMapper objectMapper,
            AiCacheMetrics metrics) {
        this.redisTemplateProvider = redisTemplateProvider;
        this.redisProperties = redisProperties;
        this.cacheProperties = cacheProperties;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
    }

    private boolean usable() {
        try {
            return redisProperties.isEnabled() && cacheProperties.isEnabled()
                    && redisTemplateProvider.getIfAvailable() != null;
        } catch (Exception exception) {
            return false;
        }
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type) {
        if (!usable() || key == null || key.isBlank()) {
            return Optional.empty();
        }
        try {
            String raw = redisTemplateProvider.getObject().opsForValue().get(key);
            if (raw == null || raw.isBlank()) {
                return Optional.empty();
            }
            if (raw.getBytes(StandardCharsets.UTF_8).length > cacheProperties.getMaxPayloadBytes()) {
                metrics.invalidPayload(cacheOf(key));
                return Optional.empty();
            }
            JsonNode envelope = objectMapper.readTree(raw);
            if (!envelope.path("schemaVersion").isInt()
                    || envelope.path("schemaVersion").asInt() != AiCacheKeyFactory.SCHEMA_VERSION) {
                metrics.invalidPayload(cacheOf(key));
                return Optional.empty();
            }
            JsonNode payload = envelope.path("payload");
            if (payload.isMissingNode() || payload.isNull()) {
                metrics.invalidPayload(cacheOf(key));
                return Optional.empty();
            }
            T value = objectMapper.treeToValue(payload, type);
            if (value == null) {
                metrics.invalidPayload(cacheOf(key));
                return Optional.empty();
            }
            return Optional.of(value);
        } catch (Exception exception) {
            metrics.error(cacheOf(key), "get");
            log.debug("Redis cache miss keyHash={} error={}", sha(key), exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        if (!usable() || key == null || key.isBlank() || value == null) {
            return;
        }
        try {
            String envelope = objectMapper.writeValueAsString(new Envelope(
                    AiCacheKeyFactory.SCHEMA_VERSION, Instant.now().toString(),
                    objectMapper.valueToTree(value)));
            if (envelope.getBytes(StandardCharsets.UTF_8).length > cacheProperties.getMaxPayloadBytes()) {
                metrics.invalidPayload(cacheOf(key));
                return;
            }
            redisTemplateProvider.getObject().opsForValue().set(key, envelope, withJitter(ttl));
            metrics.put(cacheOf(key));
        } catch (Exception exception) {
            metrics.error(cacheOf(key), "put");
            log.debug("Redis put failed keyHash={} error={}", sha(key), exception.getClass().getSimpleName());
        }
    }

    @Override
    public boolean putIfAbsent(String key, Object value, Duration ttl) {
        if (!usable() || key == null || key.isBlank() || value == null) {
            return false;
        }
        try {
            String envelope = objectMapper.writeValueAsString(new Envelope(
                    AiCacheKeyFactory.SCHEMA_VERSION, Instant.now().toString(),
                    objectMapper.valueToTree(value)));
            Boolean applied = redisTemplateProvider.getObject().opsForValue()
                    .setIfAbsent(key, envelope, withJitter(ttl));
            return Boolean.TRUE.equals(applied);
        } catch (Exception exception) {
            metrics.error(cacheOf(key), "putIfAbsent");
            return false;
        }
    }

    @Override
    public void delete(String key) {
        if (!usable() || key == null || key.isBlank()) {
            return;
        }
        try {
            redisTemplateProvider.getObject().delete(key);
        } catch (Exception exception) {
            metrics.error(cacheOf(key), "delete");
        }
    }

    @Override
    public void deleteAll(List<String> keys) {
        if (!usable() || keys == null || keys.isEmpty()) {
            return;
        }
        try {
            redisTemplateProvider.getObject().delete(keys.stream().filter(key -> key != null && !key.isBlank()).toList());
        } catch (Exception exception) {
            metrics.error("shared", "deleteAll");
        }
    }

    Duration withJitter(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return Duration.ofMinutes(5);
        }
        int jitterPercent = Math.max(0, Math.min(50, cacheProperties.getTtlJitterPercent()));
        if (jitterPercent == 0) {
            return ttl;
        }
        long millis = ttl.toMillis();
        long delta = (long) (millis * (jitterPercent / 100.0)
                * (ThreadLocalRandom.current().nextDouble() * 2 - 1));
        return Duration.ofMillis(Math.max(1000L, millis + delta));
    }

    private String cacheOf(String key) {
        // key format vm:{env}:{service}:v1:{namespace}:... -> namespace only, bounded cardinality
        try {
            String[] parts = key.split(":");
            if (parts.length >= 5) {
                return parts[4];
            }
        } catch (Exception ignored) {
            // fall through
        }
        return "shared";
    }

    private String sha(String key) {
        return AiCacheKeyFactory.sha256Hex(key).substring(0, 12);
    }

    private record Envelope(int schemaVersion, String createdAt, JsonNode payload) {
    }
}
