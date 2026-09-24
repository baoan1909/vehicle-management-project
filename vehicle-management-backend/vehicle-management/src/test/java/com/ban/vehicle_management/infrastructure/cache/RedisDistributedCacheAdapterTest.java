package com.ban.vehicle_management.infrastructure.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.application.ai.port.out.DistributedCachePortOut;
import com.ban.vehicle_management.application.ai.service.AiCacheProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class RedisDistributedCacheAdapterTest {

    private RedisDistributedCacheAdapter adapter(AiCacheProperties cacheProperties,
            ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate> provider) {
        RedisProperties redisProperties = new RedisProperties();
        return new RedisDistributedCacheAdapter(provider, redisProperties, cacheProperties,
                new ObjectMapper(), new AiCacheMetrics(new SimpleMeterRegistry()));
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate> emptyProvider() {
        return (ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate>) (ObjectProvider<?>)
                new ObjectProvider<>() {
                    @Override
                    public org.springframework.data.redis.core.StringRedisTemplate getObject() {
                        return null;
                    }

                    @Override
                    public org.springframework.data.redis.core.StringRedisTemplate getObject(Object... args) {
                        return null;
                    }

                    @Override
                    public org.springframework.data.redis.core.StringRedisTemplate getIfAvailable() {
                        return null;
                    }

                    @Override
                    public org.springframework.data.redis.core.StringRedisTemplate getIfUnique() {
                        return null;
                    }
                };
    }

    @Test
    void disabledCacheFailsOpenAsMiss() {
        AiCacheProperties cacheProperties = new AiCacheProperties();
        cacheProperties.setEnabled(false);
        RedisDistributedCacheAdapter adapter = adapter(cacheProperties, emptyProvider());
        assertTrue(adapter.get("vm:k", String.class).isEmpty());
        // Must not throw.
        adapter.put("vm:k", "v", Duration.ofMinutes(1));
        adapter.putIfAbsent("vm:k", "v", Duration.ofMinutes(1));
        adapter.delete("vm:k");
        adapter.deleteAll(List.of("vm:k"));
    }

    @Test
    void redisDownFailsOpenAsMiss() {
        AiCacheProperties cacheProperties = new AiCacheProperties();
        ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate> failing =
                new ObjectProvider<>() {
                    @Override
                    public org.springframework.data.redis.core.StringRedisTemplate getObject() {
                        throw new RuntimeException("redis down");
                    }

                    @Override
                    public org.springframework.data.redis.core.StringRedisTemplate getObject(Object... args) {
                        throw new RuntimeException("redis down");
                    }

                    @Override
                    public org.springframework.data.redis.core.StringRedisTemplate getIfAvailable() {
                        throw new RuntimeException("redis down");
                    }

                    @Override
                    public org.springframework.data.redis.core.StringRedisTemplate getIfUnique() {
                        throw new RuntimeException("redis down");
                    }
                };
        RedisDistributedCacheAdapter adapter = adapter(cacheProperties, failing);
        // getIfAvailable throws -> usable() throws? Guard via try: adapter must not propagate.
        try {
            Optional<String> result = adapter.get("vm:k", String.class);
            assertTrue(result.isEmpty());
        } catch (Exception exception) {
            // Acceptable only if fail-open guard missed; fail the test to surface it.
            assertTrue(false, "Cache get must fail open, got " + exception);
        }
    }

    @Test
    void ttlJitterStaysWithinConfiguredBand() {
        AiCacheProperties cacheProperties = new AiCacheProperties();
        cacheProperties.setTtlJitterPercent(10);
        RedisDistributedCacheAdapter adapter = adapter(cacheProperties, emptyProvider());
        Duration base = Duration.ofMinutes(15);
        for (int index = 0; index < 50; index++) {
            Duration jittered = adapter.withJitter(base);
            long millis = jittered.toMillis();
            assertTrue(millis >= base.toMillis() * 0.9 && millis <= base.toMillis() * 1.1,
                    "Jitter out of band: " + millis);
        }
    }

    @Test
    void corruptPayloadIsTreatedAsMiss() {
        AiCacheProperties cacheProperties = new AiCacheProperties();
        org.springframework.data.redis.core.StringRedisTemplate template =
                org.mockito.Mockito.mock(org.springframework.data.redis.core.StringRedisTemplate.class);
        org.springframework.data.redis.core.ValueOperations<String, String> ops =
                org.mockito.Mockito.mock(org.springframework.data.redis.core.ValueOperations.class);
        org.mockito.Mockito.when(template.opsForValue()).thenReturn(ops);
        org.mockito.Mockito.when(ops.get("vm:k")).thenReturn("not-json{{{");
        ObjectProvider<org.springframework.data.redis.core.StringRedisTemplate> provider =
                new ObjectProvider<>() {
                    @Override
                    public org.springframework.data.redis.core.StringRedisTemplate getObject() {
                        return template;
                    }

                    @Override
                    public org.springframework.data.redis.core.StringRedisTemplate getObject(Object... args) {
                        return template;
                    }

                    @Override
                    public org.springframework.data.redis.core.StringRedisTemplate getIfAvailable() {
                        return template;
                    }

                    @Override
                    public org.springframework.data.redis.core.StringRedisTemplate getIfUnique() {
                        return template;
                    }
                };
        RedisDistributedCacheAdapter adapter = adapter(cacheProperties, provider);
        DistributedCachePortOut port = adapter;
        assertTrue(port.get("vm:k", String.class).isEmpty());
        assertEquals(1, 1);
    }
}
