package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.application.ai.cache.model.CachedGroundedAnswer;
import com.ban.vehicle_management.application.ai.port.out.DistributedLockPortOut;
import com.ban.vehicle_management.application.ai.port.out.GroundedAnswerCachePortOut;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;

class AiSingleFlightServiceTest {

    static class InMemoryLock implements DistributedLockPortOut {
        private final Map<String, String> locks = new ConcurrentHashMap<>();

        @Override
        public Optional<String> acquire(String key, Duration ttl) {
            String token = java.util.UUID.randomUUID().toString();
            return locks.putIfAbsent(key, token) == null ? Optional.of(token) : Optional.empty();
        }

        @Override
        public boolean release(String key, String token) {
            return locks.remove(key, token);
        }
    }

    @Test
    void onlyOneOwnerAcquiresLock() {
        AiCacheProperties properties = new AiCacheProperties();
        com.ban.vehicle_management.infrastructure.cache.RedisProperties redisProperties =
                new com.ban.vehicle_management.infrastructure.cache.RedisProperties();
        com.ban.vehicle_management.infrastructure.cache.AiCacheKeyFactory keys =
                new com.ban.vehicle_management.infrastructure.cache.AiCacheKeyFactory(
                        redisProperties, properties);
        InMemoryLock lock = new InMemoryLock();
        GroundedAnswerCachePortOut emptyCache = new GroundedAnswerCachePortOut() {
            @Override
            public Optional<CachedGroundedAnswer> get(String key) {
                return Optional.empty();
            }

            @Override
            public void put(String key, CachedGroundedAnswer payload) {
            }
        };
        AiSingleFlightService flight = new AiSingleFlightService(lock,
                emptyCache, properties, keys);

        var first = flight.acquire("grounded-key-1");
        var second = flight.acquire("grounded-key-1");

        assertTrue(first.owner());
        assertFalse(second.owner());
    }

    @Test
    void wrongTokenCannotRelease() {
        InMemoryLock lock = new InMemoryLock();
        Optional<String> token = lock.acquire("k", Duration.ofSeconds(45));
        assertTrue(token.isPresent());
        assertFalse(lock.release("k", "wrong-token"));
        assertTrue(lock.release("k", token.get()));
    }
}
