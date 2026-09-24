package com.ban.vehicle_management.infrastructure.cache;

import com.ban.vehicle_management.application.ai.port.out.DistributedLockPortOut;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisDistributedLockAdapter implements DistributedLockPortOut {

    private static final Logger log = LoggerFactory.getLogger(RedisDistributedLockAdapter.class);
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final RedisProperties redisProperties;
    private final AiCacheMetrics metrics;

    public RedisDistributedLockAdapter(
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            RedisProperties redisProperties,
            AiCacheMetrics metrics) {
        this.redisTemplateProvider = redisTemplateProvider;
        this.redisProperties = redisProperties;
        this.metrics = metrics;
    }

    @Override
    public Optional<String> acquire(String key, Duration ttl) {
        try {
            if (!redisProperties.isEnabled() || redisTemplateProvider.getIfAvailable() == null
                    || key == null || key.isBlank()) {
                return Optional.empty();
            }
        } catch (Exception exception) {
            return Optional.empty();
        }
        try {
            String token = UUID.randomUUID().toString();
            Boolean applied = redisTemplateProvider.getObject().opsForValue()
                    .setIfAbsent(key, token, ttl == null ? Duration.ofSeconds(45) : ttl);
            if (Boolean.TRUE.equals(applied)) {
                return Optional.of(token);
            }
            metrics.singleFlightContention();
            return Optional.empty();
        } catch (Exception exception) {
            metrics.error("distributed-lock", "acquire");
            log.debug("Redis lock acquire failed error={}", exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    @Override
    public boolean release(String key, String token) {
        try {
            if (!redisProperties.isEnabled() || redisTemplateProvider.getIfAvailable() == null
                    || key == null || key.isBlank() || token == null || token.isBlank()) {
                return false;
            }
        } catch (Exception exception) {
            return false;
        }
        try {
            Long released = redisTemplateProvider.getObject().execute(
                    UNLOCK_SCRIPT, List.of(key), token);
            return Long.valueOf(1L).equals(released);
        } catch (Exception exception) {
            metrics.error("distributed-lock", "release");
            return false;
        }
    }
}
