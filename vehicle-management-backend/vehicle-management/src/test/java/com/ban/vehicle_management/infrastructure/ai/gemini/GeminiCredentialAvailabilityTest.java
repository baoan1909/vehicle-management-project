package com.ban.vehicle_management.infrastructure.ai.gemini;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.ban.vehicle_management.application.ai.port.out.DistributedCachePortOut;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;

@ExtendWith(MockitoExtension.class)
class GeminiCredentialAvailabilityTest {
    @Mock DistributedCachePortOut cache;

    @Test void dailyProjectQuotaSurvivesJsonPersistenceAndAnotherInstanceWithoutBlockingOtherProjects() throws Exception {
        var json = new ObjectMapper();
        Map<String, String> persisted = new HashMap<>();
        doAnswer(call -> {
            persisted.put(call.getArgument(0), json.writeValueAsString(call.getArgument(1)));
            return null;
        }).when(cache).put(anyString(), any(), any());
        when(cache.get(anyString(), eq(GeminiCredentialPool.Availability.class))).thenAnswer(call -> {
            String value = persisted.get(call.getArgument(0));
            return value == null ? Optional.empty() : Optional.of(json.readValue(value, GeminiCredentialPool.Availability.class));
        });
        Clock fixed = Clock.fixed(Instant.parse("2026-11-20T09:00:00Z"), ZoneOffset.UTC);
        var properties = new GeminiProperties(); properties.setApiKey("synthetic-first"); properties.setProjectId("real-project-alpha");
        var first = new GeminiCredentialPool(properties, new MockEnvironment(), cache, fixed);
        first.failure(first.credentials().getFirst(), GeminiCredentialPool.GENERATION, "model-a", 429,
                "{\"error\":{\"details\":[{\"violations\":[{\"quotaId\":\"RequestsPerDayPerProjectPerModel\"}]}]}}", null);
        properties = new GeminiProperties(); properties.setApiKey("synthetic-second"); properties.setProjectId("real-project-alpha");
        var restarted = new GeminiCredentialPool(properties, new MockEnvironment(), cache, fixed);
        assertNull(restarted.acquire(restarted.credentials().getFirst(), GeminiCredentialPool.GENERATION, "model-a", restarted.deadline()));
        assertTrue(persisted.values().stream().anyMatch(value -> value.contains("2026-11-21T08:00:00Z")));
        properties = new GeminiProperties(); properties.setApiKey("synthetic-third"); properties.setProjectId("real-project-beta");
        var independent = new GeminiCredentialPool(properties, new MockEnvironment(), cache, fixed);
        try (var lease = independent.acquire(independent.credentials().getFirst(), GeminiCredentialPool.GENERATION, "model-a", independent.deadline())) {
            assertNotNull(lease);
        }
    }
}
