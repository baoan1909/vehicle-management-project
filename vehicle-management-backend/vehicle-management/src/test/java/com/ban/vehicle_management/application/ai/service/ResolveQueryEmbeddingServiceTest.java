package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.ai.port.out.QueryEmbeddingCachePortOut;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.EmbeddingRequest;
import com.ban.vehicle_management.domain.ai.model.EmbeddingResult;
import com.ban.vehicle_management.domain.ai.model.EmbeddingVector;
import com.ban.vehicle_management.infrastructure.cache.AiCacheKeyFactory;
import com.ban.vehicle_management.infrastructure.cache.RedisProperties;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResolveQueryEmbeddingServiceTest {

    @Mock
    private EmbeddingService embeddingService;
    @Mock
    private QueryEmbeddingCachePortOut embeddingCache;

    private ResolveQueryEmbeddingService service;
    private AiCacheProperties cacheProperties;
    private AiCacheKeyFactory keyFactory;

    @BeforeEach
    void setUp() {
        cacheProperties = new AiCacheProperties();
        cacheProperties.setKeyHmacSecret("test-secret-32bytes-minimum-length!!");
        RedisProperties redisProperties = new RedisProperties();
        keyFactory = new AiCacheKeyFactory(redisProperties, cacheProperties);
        service = new ResolveQueryEmbeddingService(embeddingService, embeddingCache,
                keyFactory, new SensitiveQueryGuard(), cacheProperties);
    }

    private AiModelConfiguration configuration() {
        AiModelConfiguration configuration = new AiModelConfiguration();
        configuration.setProvider(AiProvider.GEMINI);
        configuration.setModelId("gemini-embedding-001");
        return configuration;
    }

    @Test
    void cacheHitDoesNotCallProvider() {
        EmbeddingVector vector = EmbeddingVector.of(new double[4], 4);
        when(embeddingCache.get(anyString(), any(int.class))).thenReturn(Optional.of(vector));

        EmbeddingResult result = service.embedQuery("formatted", "quy trinh the xe",
                configuration(), 4, "rag-qa-v1");

        assertTrue(result.isSuccess());
        assertEquals(4, result.getVector().dimension());
        verify(embeddingService, never()).embed(any(EmbeddingRequest.class), any());
    }

    @Test
    void cacheMissCallsProviderOnceAndStores() {
        when(embeddingCache.get(anyString(), any(int.class))).thenReturn(Optional.empty());
        EmbeddingVector vector = EmbeddingVector.of(new double[]{1, 2, 3, 4}, 4);
        when(embeddingService.embed(any(), any())).thenReturn(EmbeddingResult.success(vector, "m"));

        EmbeddingResult result = service.embedQuery("formatted", "quy trinh the xe",
                configuration(), 4, "rag-qa-v1");

        assertTrue(result.isSuccess());
        verify(embeddingService).embed(any(EmbeddingRequest.class), any());
        verify(embeddingCache).put(anyString(), any(), any());
    }

    @Test
    void wrongDimensionIsTreatedAsMiss() {
        EmbeddingVector wrong = EmbeddingVector.of(new double[8], 8);
        when(embeddingCache.get(anyString(), any(int.class))).thenReturn(Optional.empty());
        when(embeddingService.embed(any(), any()))
                .thenReturn(EmbeddingResult.success(EmbeddingVector.of(new double[4], 4), "m"));

        service.embedQuery("formatted", "quy trinh the xe", configuration(), 4, "rag-qa-v1");

        verify(embeddingService).embed(any(), any());
    }

    @Test
    void piiQueryBypassesCache() {
        EmbeddingVector vector = EmbeddingVector.of(new double[4], 4);
        when(embeddingService.embed(any(), any())).thenReturn(EmbeddingResult.success(vector, "m"));

        service.embedQuery("formatted", "lien he user@example.com giup toi", configuration(), 4, "rag-qa-v1");

        verify(embeddingCache, never()).get(anyString(), any(int.class));
        verify(embeddingCache, never()).put(anyString(), any(), any());
    }

    @Test
    void providerFailureIsNotCached() {
        when(embeddingCache.get(anyString(), any(int.class))).thenReturn(Optional.empty());
        when(embeddingService.embed(any(), any())).thenReturn(EmbeddingResult.failure(
                new com.ban.vehicle_management.domain.ai.model.EmbeddingFailure(
                        "TIMEOUT", null, null, null, true, null)));

        EmbeddingResult result = service.embedQuery("formatted", "quy trinh the xe",
                configuration(), 4, "rag-qa-v1");

        assertTrue(!result.isSuccess());
        verify(embeddingCache, never()).put(anyString(), any(), any());
    }

    @Test
    void differentModelProducesDifferentKey() {
        AiModelConfiguration first = configuration();
        first.setModelId("model-a");
        AiModelConfiguration second = configuration();
        second.setModelId("model-b");
        String keyA = keyFactory.embeddingKey("GEMINI", "model-a", 4, "rag-qa-v1", "quy trinh");
        String keyB = keyFactory.embeddingKey("GEMINI", "model-b", 4, "rag-qa-v1", "quy trinh");
        assertTrue(!keyA.equals(keyB));
    }
}
