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

    void delete(String key);

    void deleteAll(List<String> keys);
}
