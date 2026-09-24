package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.ai.cache.model.CachedCitation;
import com.ban.vehicle_management.application.ai.cache.model.CachedGroundedAnswer;
import com.ban.vehicle_management.application.ai.port.out.GroundedAnswerCachePortOut;
import com.ban.vehicle_management.infrastructure.cache.AiCacheMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroundedAnswerCacheAdapterTest {

    @Mock
    private com.ban.vehicle_management.application.ai.port.out.DistributedCachePortOut distributedCache;

    private GroundedAnswerCacheAdapter adapter;
    private AiCacheProperties cacheProperties;

    @BeforeEach
    void setUp() {
        cacheProperties = new AiCacheProperties();
        adapter = new GroundedAnswerCacheAdapter(distributedCache, cacheProperties,
                new AiCacheMetrics(new SimpleMeterRegistry()));
    }

    private CachedGroundedAnswer answer(UUID indexId) {
        return new CachedGroundedAnswer(1, Instant.now(), "Xe vao lan IN.",
                List.of(new CachedCitation(UUID.randomUUID(), UUID.randomUUID(), "C1", "T", 1, "S",
                        new java.math.BigDecimal("0.9"))),
                indexId, "checksum", "support-assistant-v1", "fp", "scope", "tenant", "vi", 0.9, false);
    }

    @Test
    void validHitIsReturned() {
        UUID indexId = UUID.randomUUID();
        when(distributedCache.get(anyString(), any())).thenReturn(Optional.of(answer(indexId)));

        Optional<CachedGroundedAnswer> hit = adapter.get("key");

        assertTrue(hit.isPresent());
        assertEquals("Xe vao lan IN.", hit.get().responseText());
    }

    @Test
    void invalidCitationLabelIsRejected() {
        UUID indexId = UUID.randomUUID();
        CachedGroundedAnswer poisoned = new CachedGroundedAnswer(1, Instant.now(), "Text",
                List.of(new CachedCitation(UUID.randomUUID(), UUID.randomUUID(), "EVIL", "T", 1, "S",
                        new java.math.BigDecimal("0.9"))),
                indexId, "checksum", "v1", "fp", "scope", "tenant", "vi", 0.9, false);
        when(distributedCache.get(anyString(), any())).thenReturn(Optional.of(poisoned));

        assertTrue(adapter.get("key").isEmpty());
        verify(distributedCache).delete(anyString());
    }

    @Test
    void putUsesTierTtl() {
        UUID indexId = UUID.randomUUID();
        adapter.put("key", answer(indexId), "PUBLIC");
        verify(distributedCache).put(anyString(), any(), any(Duration.class));
    }

    @Test
    void cacheDisabledFallsBack() {
        cacheProperties.setEnabled(false);
        assertTrue(adapter.get("key").isEmpty());
    }
}
