package com.ban.vehicle_management.infrastructure.security.guestsupport;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.operations.guestsupport.config.GuestSupportProperties;
import com.ban.vehicle_management.application.operations.guestsupport.model.GuestRateLimitBucket;
import com.ban.vehicle_management.infrastructure.cache.RedisProperties;
import com.ban.vehicle_management.shared.exception.ServiceUnavailableException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisGuestRateLimitAdapterTest {

    @Test
    void twoAdapterInstancesShareTheRedisDecision() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(), any(), any(String.class))).thenReturn("1:60000", "2:59000");
        ObjectProvider<StringRedisTemplate> provider = provider(redis);
        RedisGuestRateLimitAdapter first = adapter(provider, false);
        RedisGuestRateLimitAdapter second = adapter(provider, false);
        String digest = "a".repeat(64);

        assertTrue(first.consume(GuestRateLimitBucket.ASSISTANT_IP, digest, 1, Duration.ofMinutes(1)).allowed());
        assertFalse(second.consume(GuestRateLimitBucket.ASSISTANT_IP, digest, 1, Duration.ofMinutes(1)).allowed());
    }

    @Test
    void redisUnavailableFailsClosedUnlessFallbackExplicitlyEnabled() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(), any(), any(String.class))).thenThrow(new RuntimeException("redis down"));
        String digest = "b".repeat(64);

        assertThrows(ServiceUnavailableException.class, () -> adapter(provider(redis), false)
                .consume(GuestRateLimitBucket.ASSISTANT_SESSION, digest, 1, Duration.ofMinutes(1)));
        assertTrue(adapter(provider(redis), true)
                .consume(GuestRateLimitBucket.ASSISTANT_SESSION, digest, 1, Duration.ofMinutes(1)).allowed());
    }

    @Test
    void rawIdentityIsRejectedBeforeItCanBecomeARedisKey() {
        assertThrows(IllegalArgumentException.class, () -> adapter(provider(mock(StringRedisTemplate.class)), false)
                .consume(GuestRateLimitBucket.ASSISTANT_IP, "127.0.0.1", 1, Duration.ofMinutes(1)));
    }

    private RedisGuestRateLimitAdapter adapter(
            ObjectProvider<StringRedisTemplate> provider,
            boolean fallback
    ) {
        RedisProperties redis = new RedisProperties();
        GuestSupportProperties guest = new GuestSupportProperties();
        guest.setRateLimitLocalFallbackEnabled(fallback);
        return new RedisGuestRateLimitAdapter(redis, guest, provider,
                new GuestSupportSecurityMetrics(new SimpleMeterRegistry()));
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<StringRedisTemplate> provider(StringRedisTemplate template) {
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(template);
        return provider;
    }
}
