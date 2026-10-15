package com.ban.vehicle_management.application.ai.port.out;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Shared typed distributed-cache port. Implementations must fail open: any
 * transport, timeout or deserialization failure is reported as a miss, never
 * as an exception to business flows.
 */
public interface DistributedCachePortOut {

    <T> Optional<T> get(String key, Class<T> type);

    void put(String key, Object value, Duration ttl);

    boolean putIfAbsent(String key, Object value, Duration ttl);

    /**
     * Atomically increments a numeric value and starts its TTL when the key is
     * first created. Returns empty when the cache is unavailable.
     */
    Optional<Long> increment(String key, Duration ttl);

    /**
     * Reads a counter created by {@link #increment(String, Duration)}.
     * Returns empty when the key is absent, invalid or the cache is unavailable.
     */
    Optional<Long> getCounter(String key);

    void delete(String key);

    void deleteAll(List<String> keys);
}
