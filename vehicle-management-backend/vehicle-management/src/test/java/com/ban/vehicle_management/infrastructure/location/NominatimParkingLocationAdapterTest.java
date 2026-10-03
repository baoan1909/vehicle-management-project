package com.ban.vehicle_management.infrastructure.location;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.infrastructure.cache.RedisProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import static org.mockito.Mockito.mock;

class NominatimParkingLocationAdapterTest {

    @Test
    void shouldRejectProviderOutsideFixedHttpsAllowlist() {
        NominatimProperties properties = new NominatimProperties();
        properties.setBaseUrl("https://169.254.169.254/latest/meta-data");

        assertThrows(IllegalArgumentException.class, () -> new NominatimParkingLocationAdapter(
                properties,
                new RedisProperties(),
                emptyRedisProvider(),
                new ObjectMapper(),
                mock(MapProviderQuotaGuard.class),
                mock(MapProviderMetrics.class)
        ));
    }

    @Test
    void shouldRequireContactAndPositiveDailyLimit() {
        NominatimProperties properties = new NominatimProperties();
        assertTrue(properties.isValid());

        properties.setUserAgent("CoParking/1.0");
        assertFalse(properties.isValid());

        properties.setUserAgent("CoParking/1.0 (contact: dev@coparking.local)");
        properties.setDailyRequestLimit(0);
        assertFalse(properties.isValid());

        properties.setDailyRequestLimit(1_000);
        properties.setQuotaUsagePercent(96);
        assertFalse(properties.isValid());
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<StringRedisTemplate> emptyRedisProvider() {
        return org.mockito.Mockito.mock(ObjectProvider.class);
    }
}
