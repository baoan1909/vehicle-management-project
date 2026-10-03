package com.ban.vehicle_management.infrastructure.security.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.ban.vehicle_management.infrastructure.cache.RedisProperties;
import com.ban.vehicle_management.shared.exception.MapRequestLimitException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class MapApiRateLimiterTest {

    @Test
    void shouldApplyLowerLimitToAnonymousIp() {
        MapApiRateLimitProperties properties = properties();
        properties.setAnonymousIpLimit(2);
        MapApiRateLimiter limiter = limiter(properties);

        limiter.check("203.0.113.10", null);
        limiter.check("203.0.113.10", null);
        MapRequestLimitException exception = assertThrows(
                MapRequestLimitException.class,
                () -> limiter.check("203.0.113.10", null)
        );

        assertEquals("MAP_RATE_LIMIT_EXCEEDED", exception.code());
    }

    @Test
    void shouldLimitAccountAcrossDifferentIpAddresses() {
        MapApiRateLimitProperties properties = properties();
        properties.setAccountLimit(1);
        MapApiRateLimiter limiter = limiter(properties);
        UUID accountId = UUID.randomUUID();

        limiter.check("203.0.113.11", accountId);

        assertThrows(
                MapRequestLimitException.class,
                () -> limiter.check("203.0.113.12", accountId)
        );
    }

    private MapApiRateLimitProperties properties() {
        MapApiRateLimitProperties properties = new MapApiRateLimitProperties();
        properties.setAnonymousIpLimit(2);
        properties.setAuthenticatedIpLimit(10);
        properties.setAccountLimit(10);
        return properties;
    }

    @SuppressWarnings("unchecked")
    private MapApiRateLimiter limiter(MapApiRateLimitProperties properties) {
        RedisProperties redisProperties = new RedisProperties();
        redisProperties.setEnabled(false);
        return new MapApiRateLimiter(properties, redisProperties, mock(ObjectProvider.class));
    }
}
