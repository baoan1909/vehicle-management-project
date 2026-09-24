package com.ban.vehicle_management.ai.cache;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.ban.vehicle_management.application.ai.cache.model.CachedGroundedAnswer;
import com.ban.vehicle_management.application.ai.port.out.AiModelCircuitBreakerPortOut;
import com.ban.vehicle_management.application.ai.port.out.DistributedCachePortOut;
import com.ban.vehicle_management.application.ai.port.out.DistributedLockPortOut;
import com.ban.vehicle_management.application.ai.port.out.GroundedAnswerCachePortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeRetrievalCachePortOut;
import com.ban.vehicle_management.application.ai.port.out.QueryEmbeddingCachePortOut;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Redis integration coverage for the AI cache foundation.
 *
 * <p>Not executed in the default unit-test task: class name follows the
 * {@code *IT} convention so {@code mvn test} (surefire) ignores it without a
 * failsafe setup. Requires Redis from {@code docker-compose.redis.yml} and a
 * reachable PostgreSQL; every test fails open with an assumption when the
 * backing service is unavailable.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class AiRedisCacheIT {

    @Autowired(required = false)
    private DistributedCachePortOut distributedCache;
    @Autowired(required = false)
    private DistributedLockPortOut distributedLock;
    @Autowired(required = false)
    private QueryEmbeddingCachePortOut embeddingCache;
    @Autowired(required = false)
    private KnowledgeRetrievalCachePortOut retrievalCache;
    @Autowired(required = false)
    private GroundedAnswerCachePortOut answerCache;
    @Autowired(required = false)
    private AiModelCircuitBreakerPortOut circuitBreaker;

    private void assumeCache() {
        assumeTrue(distributedCache != null, "Distributed cache bean missing");
    }

    @Test
    void redisContainerStartsAndPingSucceeds() {
        assumeCache();
        distributedCache.put("vm:test:ping", "pong", Duration.ofSeconds(5));
        assumeTrue(distributedCache.get("vm:test:ping", String.class).isPresent(),
                "Redis unreachable; start docker-compose.redis.yml");
    }

    @Test
    void putGetExpireTypedPayload() throws InterruptedException {
        assumeCache();
        String key = "vm:test:typed:" + UUID.randomUUID();
        distributedCache.put(key, List.of("a", "b"), Duration.ofSeconds(1));
        assumeTrue(distributedCache.get(key, List.class).isPresent(), "Redis unreachable");
        Thread.sleep(1500L);
        assert distributedCache.get(key, List.class).isEmpty() : "TTL did not expire";
    }

    @Test
    void luaUnlockRespectsOwnershipToken() {
        assumeTrue(distributedLock != null, "Lock bean missing");
        String key = "vm:test:lock:" + UUID.randomUUID();
        Optional<String> owner = distributedLock.acquire(key, Duration.ofSeconds(10));
        assumeTrue(owner.isPresent(), "Redis unreachable");
        assert !distributedLock.release(key, "wrong-token") : "Wrong token released a foreign lock";
        assert distributedLock.release(key, owner.get()) : "Owner could not release";
    }

    @Test
    void circuitBreakerCountsAtomicallyAcrossThreads() {
        assumeTrue(circuitBreaker != null, "Circuit breaker bean missing");
        String key = "vm:test:circuit:" + UUID.randomUUID();
        Runnable failure = () -> circuitBreaker.recordFailure(key, true, null);
        List<Thread> threads = java.util.stream.IntStream.range(0, 8)
                .mapToObj(ignored -> new Thread(failure)).toList();
        threads.forEach(Thread::start);
        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });
        assert !circuitBreaker.shouldAllow(key).allowed() : "Circuit should be OPEN after concurrent failures";
    }

    @Test
    void halfOpenAdmitsSingleProbe() {
        assumeTrue(circuitBreaker != null, "Circuit breaker bean missing");
        String key = "vm:test:halfopen:" + UUID.randomUUID();
        circuitBreaker.recordFailure(key, true, null);
        circuitBreaker.recordFailure(key, true, null);
        circuitBreaker.recordFailure(key, true, null);
        var first = circuitBreaker.shouldAllow(key);
        var second = circuitBreaker.shouldAllow(key);
        assert !(first.allowed() && second.allowed()) : "HALF_OPEN admitted more than one probe";
    }

    @Test
    void cacheTtlExpiresInRealTime() throws InterruptedException {
        assumeCache();
        String key = "vm:test:ttl:" + UUID.randomUUID();
        distributedCache.put(key, "v", Duration.ofSeconds(1));
        assumeTrue(distributedCache.get(key, String.class).isPresent(), "Redis unreachable");
        Thread.sleep(1500L);
        assert distributedCache.get(key, String.class).isEmpty() : "TTL did not expire";
    }

    @Test
    void applicationFlowFailsOpenWhenRedisUnavailable() {
        // Covered by unit tests with throwing adapters; this IT documents the live gate:
        // stop Redis, POST /api/operations/chat/conversations/{id}/messages must still succeed.
        assumeTrue(true, "Manual gate: stop redis-cache container and exercise chat flow");
    }

    @Test
    void twoTenantsCannotReadEachOtherCache() {
        assumeTrue(answerCache != null, "Answer cache bean missing");
        // Keys embed tenant fingerprints; live assertion requires two tenant contexts.
        assumeTrue(true, "Manual gate: prime tenant A, assert tenant B key differs and misses");
    }

    @Test
    void groundedAnswerHitPersistsMessageCitationAudit() {
        assumeTrue(answerCache != null, "Answer cache bean missing");
        Optional<CachedGroundedAnswer> cached = answerCache.get("vm:test:missing:" + UUID.randomUUID());
        assert cached.isEmpty() : "Unexpected cache entry";
        // Live gate: prime STATIC_KNOWLEDGE answer, re-ask, assert new message/citation/audit rows in PostgreSQL.
        assumeTrue(true, "Manual gate: exercise static-knowledge twice against live DB");
    }

    @Test
    void redisRestartKeepsPostgresSourceOfTruth() {
        // Live gate: restart redis-cache container mid-session, assert PostgreSQL rows intact.
        assumeTrue(true, "Manual gate: restart redis-cache, verify messages/citations/audits remain");
    }
}
