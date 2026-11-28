package com.ban.vehicle_management.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.*;

import com.ban.vehicle_management.application.ai.port.out.AiProviderPortOut;
import com.ban.vehicle_management.application.ai.port.out.EmbeddingProviderPortOut;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@SpringBootTest(properties = {
        "app.ai.assistant-enabled=false", "app.ai.embedding.enabled=false",
        "app.ai.ingestion.enabled=false", "app.ai.cache.enabled=false", "app.redis.enabled=false",
        "app.ai.model-catalog-sync-initial-delay-ms=3600000"
})
@ActiveProfiles("test")
class GoogleOnlyApplicationContextTest extends PostgresIntegrationTestSupport {
    @Autowired ApplicationContext context;
    @Autowired RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Test
    void onlyGoogleGenerationAndEmbeddingProvidersAreRegistered() {
        var providers = context.getBeansOfType(AiProviderPortOut.class).values();
        var embeddings = context.getBeansOfType(EmbeddingProviderPortOut.class).values();
        assertEquals(1, providers.size());
        assertEquals(1, embeddings.size());
        assertEquals(AiProvider.GEMINI, providers.iterator().next().provider());
        assertEquals(AiProvider.GEMINI, embeddings.iterator().next().provider());
    }

    @Test
    void removedProviderSwitchEndpointIsNotMapped() {
        assertTrue(requestMappingHandlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(mapping -> mapping.getPatternValues().stream())
                .noneMatch(path -> path.equals("/api/ai/runtime") || path.equals("/ai/runtime")));
    }
}
