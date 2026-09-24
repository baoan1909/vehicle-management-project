package com.ban.vehicle_management.infrastructure.cache;

import java.time.Duration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis health contributor. Never exposes host, password, keys or values.
 */
@Component("redisCache")
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisProperties properties;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    public RedisHealthIndicator(RedisProperties properties,
            ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.properties = properties;
        this.redisTemplateProvider = redisTemplateProvider;
    }

    @Override
    public Health health() {
        try {
            if (!properties.isEnabled()) {
                return Health.up()
                        .withDetail("enabled", false)
                        .withDetail("mode", "disabled")
                        .withDetail("degraded", true).build();
            }
        } catch (Exception exception) {
            return Health.up().withDetail("enabled", true).withDetail("degraded", true).build();
        }
        StringRedisTemplate template;
        try {
            template = redisTemplateProvider.getIfAvailable();
        } catch (Exception exception) {
            return Health.up()
                    .withDetail("enabled", true)
                    .withDetail("reachable", false)
                    .withDetail("mode", "degraded")
                    .withDetail("degraded", true).build();
        }
        if (template == null) {
            return Health.up()
                    .withDetail("enabled", true)
                    .withDetail("reachable", false)
                    .withDetail("mode", "degraded")
                    .withDetail("degraded", true).build();
        }
        long started = System.nanoTime();
        try {
            String pong = template.execute(connection -> {
                try {
                    return new String(connection.ping());
                } catch (Exception exception) {
                    return null;
                }
            }, false);
            long latencyMs = Duration.ofNanos(System.nanoTime() - started).toMillis();
            boolean reachable = pong != null;
            if (!reachable) {
                return Health.up()
                        .withDetail("enabled", true)
                        .withDetail("reachable", false)
                        .withDetail("mode", "degraded")
                        .withDetail("degraded", true)
                        .withDetail("latencyMs", latencyMs).build();
            }
            return Health.up()
                    .withDetail("enabled", true)
                    .withDetail("reachable", true)
                    .withDetail("mode", "full")
                    .withDetail("degraded", false)
                    .withDetail("latencyMs", latencyMs).build();
        } catch (Exception exception) {
            long latencyMs = Duration.ofNanos(System.nanoTime() - started).toMillis();
            return Health.up()
                    .withDetail("enabled", true)
                    .withDetail("reachable", false)
                    .withDetail("mode", "degraded")
                    .withDetail("degraded", true)
                    .withDetail("latencyMs", latencyMs).build();
        }
    }
}
