package com.ban.vehicle_management.infrastructure.location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.ban.vehicle_management.infrastructure.cache.RedisProperties;
import com.ban.vehicle_management.shared.exception.MapRequestLimitException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

class MapProviderQuotaGuardTest {

    @Test
    void shouldApplyInternalNinetyPercentLimitAndReturnRetryTime() {
        MapProviderQuotaGuard guard = localGuard();

        for (int index = 0; index < 90; index++) {
            MapProviderQuotaGuard.QuotaSnapshot snapshot = guard.acquire(
                    "quota-unit",
                    MapProviderOperation.FORWARD,
                    100,
                    90
            );
            assertEquals(90 - index - 1L, snapshot.remaining());
        }

        MapRequestLimitException exception = assertThrows(
                MapRequestLimitException.class,
                () -> guard.acquire("quota-unit", MapProviderOperation.FORWARD, 100, 90)
        );
        assertEquals("MAP_DAILY_QUOTA_EXHAUSTED", exception.code());
        assertTrue(exception.retryAfter().toLocalDate().isAfter(LocalDate.now(MapProviderQuotaGuard.VIETNAM_ZONE)));
    }

    @Test
    void shouldNeverExceedLimitUnderConcurrentIncrements() throws InterruptedException {
        MapProviderQuotaGuard guard = localGuard();
        int attempts = 1_000;
        int expectedAccepted = 950;
        AtomicInteger accepted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(24);
        List<Runnable> tasks = new ArrayList<>();
        for (int index = 0; index < attempts; index++) {
            tasks.add(() -> {
                try {
                    start.await();
                    guard.acquire("quota-concurrent", MapProviderOperation.REVERSE, 1_000, 95);
                    accepted.incrementAndGet();
                } catch (MapRequestLimitException exception) {
                    rejected.incrementAndGet();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        tasks.forEach(executor::submit);
        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(expectedAccepted, accepted.get());
        assertEquals(attempts - expectedAccepted, rejected.get());
    }

    @Test
    void shouldUseRequiredProviderOperationDateKeyFormat() {
        assertEquals(
                "maps:quota:nominatim:forward:2026-09-28",
                MapProviderQuotaGuard.quotaKey(
                        "Nominatim",
                        MapProviderOperation.FORWARD,
                        LocalDate.of(2026, 9, 28)
                )
        );
    }

    @SuppressWarnings("unchecked")
    private MapProviderQuotaGuard localGuard() {
        RedisProperties redisProperties = new RedisProperties();
        redisProperties.setEnabled(false);
        return new MapProviderQuotaGuard(
                redisProperties,
                mock(ObjectProvider.class),
                new MapProviderMetrics(new SimpleMeterRegistry())
        );
    }
}
