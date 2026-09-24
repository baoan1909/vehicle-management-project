package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.port.out.DistributedLockPortOut;
import com.ban.vehicle_management.application.ai.port.out.GroundedAnswerCachePortOut;
import com.ban.vehicle_management.application.ai.cache.model.CachedGroundedAnswer;
import com.ban.vehicle_management.infrastructure.cache.AiCacheKeyFactory;
import java.time.Duration;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Single-flight guard against cache stampedes. At most one instance calls
 * Gemini for a given grounded-answer key; contenders re-read the cache after a
 * short jittered wait and then fall through fail-open.
 */
@Service
public class AiSingleFlightService {

    private final DistributedLockPortOut lockPort;
    private final GroundedAnswerCachePortOut answerCache;
    private final AiCacheProperties cacheProperties;
    private final AiCacheKeyFactory keyFactory;

    public AiSingleFlightService(DistributedLockPortOut lockPort,
            GroundedAnswerCachePortOut answerCache,
            AiCacheProperties cacheProperties,
            AiCacheKeyFactory keyFactory) {
        this.lockPort = lockPort;
        this.answerCache = answerCache;
        this.cacheProperties = cacheProperties;
        this.keyFactory = keyFactory;
    }

    public record Flight(String lockKey, String token, boolean owner) {
    }

    public Flight acquire(String groundedAnswerKey) {
        if (!cacheProperties.isEnabled()) {
            return new Flight(null, null, true);
        }
        String lockKey = keyFactory.singleFlightKey(groundedAnswerKey);
        Duration ttl = cacheProperties.getSingleFlightLockTtl() == null ? Duration.ofSeconds(45)
                : cacheProperties.getSingleFlightLockTtl();
        Optional<String> token = lockPort.acquire(lockKey, ttl);
        return token.map(value -> new Flight(lockKey, value, true))
                .orElseGet(() -> new Flight(lockKey, null, false));
    }

    public Optional<CachedGroundedAnswer> waitAndReRead(String groundedAnswerKey) {
        if (!cacheProperties.isEnabled()) {
            return Optional.empty();
        }
        Duration wait = cacheProperties.getSingleFlightWait() == null ? Duration.ofSeconds(2)
                : cacheProperties.getSingleFlightWait();
        long waitMs = Math.min(2000L, Math.max(100L, wait.toMillis()));
        long jitter = (long) (waitMs * 0.2 * (Math.random() * 2 - 1));
        try {
            Thread.sleep(Math.max(50L, waitMs + jitter));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
        return answerCache.get(groundedAnswerKey);
    }

    public void release(Flight flight) {
        if (flight == null || !flight.owner() || flight.lockKey() == null || flight.token() == null) {
            return;
        }
        lockPort.release(flight.lockKey(), flight.token());
    }
}
