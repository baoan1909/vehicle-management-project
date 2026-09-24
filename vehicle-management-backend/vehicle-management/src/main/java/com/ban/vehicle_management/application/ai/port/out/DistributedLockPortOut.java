package com.ban.vehicle_management.application.ai.port.out;

import java.time.Duration;
import java.util.Optional;

/**
 * Shared distributed-lock port with mandatory ownership token semantics.
 */
public interface DistributedLockPortOut {

    Optional<String> acquire(String key, Duration ttl);

    boolean release(String key, String token);
}
