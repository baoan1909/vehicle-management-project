package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.ai.cache.model.CachedHybridRow;
import com.ban.vehicle_management.application.ai.cache.model.CachedRetrievalPayload;
import com.ban.vehicle_management.application.ai.port.out.AiModelConfigurationPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeIndexVersionPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeRetrievalAuditPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeRetrievalCachePortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeRetrievalPortOut;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.EmbeddingResult;
import com.ban.vehicle_management.domain.ai.model.EmbeddingVector;
import com.ban.vehicle_management.domain.ai.model.HybridSearchRow;
import com.ban.vehicle_management.domain.ai.model.KnowledgeIndexVersion;
import com.ban.vehicle_management.domain.ai.model.KnowledgeRetrievalContext;
import com.ban.vehicle_management.domain.ai.model.KnowledgeRetrievalResult;
import com.ban.vehicle_management.domain.ai.model.RetrievalAudit;
import com.ban.vehicle_management.infrastructure.cache.AiCacheKeyFactory;
import com.ban.vehicle_management.infrastructure.cache.RedisProperties;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import com.ban.vehicle_management.shared.enumeration.ai.AiUseCase;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIndexVersionStatus;
import java.math.BigDecimal;
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
class KnowledgeRetrievalCacheIntegrationTest {

    @Mock
    private KnowledgeRetrievalPortOut retrievalPortOut;
    @Mock
    private KnowledgeIndexVersionPortOut indexVersionPortOut;
    @Mock
    private AiModelConfigurationPortOut configurationPortOut;
    @Mock
    private KnowledgeRetrievalAuditPortOut auditPortOut;
    @Mock
    private EmbeddingService embeddingService;
    @Mock
    private EmbeddingPromptFormatter promptFormatter;
    @Mock
    private KnowledgeAccessContextResolver accessContextResolver;
    @Mock
    private KnowledgeRetrievalCachePortOut retrievalCache;
    @Mock
    private ResolveQueryEmbeddingService queryEmbeddingService;

    private KnowledgeRetrievalService service;
    private AiCacheProperties cacheProperties;
    private AiCacheKeyFactory keyFactory;

    @BeforeEach
    void setUp() {
        EmbeddingProperties embeddingProperties = new EmbeddingProperties();
        embeddingProperties.setEnabled(true);
        RetrievalProperties retrievalProperties = new RetrievalProperties();
        cacheProperties = new AiCacheProperties();
        cacheProperties.setKeyHmacSecret("test-secret-32bytes-minimum-length!!");
        keyFactory = new AiCacheKeyFactory(new RedisProperties(), cacheProperties);
        service = new KnowledgeRetrievalService(retrievalPortOut, indexVersionPortOut,
                configurationPortOut, auditPortOut, embeddingService, promptFormatter,
                new PiiRedactionService(), embeddingProperties, retrievalProperties,
                accessContextResolver, queryEmbeddingService, retrievalCache, keyFactory,
                new SensitiveQueryGuard(), cacheProperties, null);
        org.mockito.Mockito.lenient().when(promptFormatter.supports(anyString())).thenReturn(true);
        org.mockito.Mockito.lenient().when(promptFormatter.formatQuery(anyString(), anyString()))
                .thenReturn("formatted");
        org.mockito.Mockito.lenient().when(auditPortOut.save(any(RetrievalAudit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private KnowledgeIndexVersion active() {
        KnowledgeIndexVersion version = KnowledgeIndexVersion.draft("IDX-1", UUID.randomUUID(),
                AiProvider.GEMINI, "gemini-embedding-2", 4, "chunker-v1", "rag-qa-v1",
                "COSINE", "NONE", "checksum-1", null, Instant.now());
        version.startBuild(null, Instant.now());
        version.markReady(Instant.now());
        version.setStatus(KnowledgeIndexVersionStatus.ACTIVE);
        return version;
    }

    private AiModelConfiguration embeddingConfiguration(KnowledgeIndexVersion active) {
        AiModelConfiguration configuration = new AiModelConfiguration();
        configuration.setConfigurationId(active.getModelConfigurationId());
        configuration.setProvider(AiProvider.GEMINI);
        configuration.setUseCase(AiUseCase.EMBEDDING);
        configuration.setModelId("gemini-embedding-2");
        configuration.setOutputDimension(4);
        configuration.setStatus(AiModelStatus.ACTIVE);
        return configuration;
    }

    @Test
    void cacheHitSkipsHybridSearchButCreatesNewAudit() {
        KnowledgeIndexVersion active = active();
        AiModelConfiguration configuration = embeddingConfiguration(active);
        org.mockito.Mockito.lenient().when(indexVersionPortOut.findActive()).thenReturn(Optional.of(active));
        org.mockito.Mockito.lenient().when(configurationPortOut.findById(any())).thenReturn(Optional.of(configuration));
        UUID documentId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();
        CachedRetrievalPayload payload = new CachedRetrievalPayload(1, Instant.now(),
                active.getIndexVersionId(), "checksum-1", "retrieval-policy-v1",
                "retrieval-threshold-v1", keyFactory.scopeFingerprint(List.of("PUBLIC")),
                keyFactory.tenantFingerprint(null), 5,
                List.of(new CachedHybridRow(documentId, chunkId, "T", "quy trinh the xe day du noi dung",
                        null, 1, "S1", 1, 1, BigDecimal.valueOf(0.9),
                        BigDecimal.valueOf(0.8), BigDecimal.valueOf(0.05), 1)),
                0.9, true, false, null);
        when(retrievalCache.get(anyString())).thenReturn(Optional.of(payload));

        KnowledgeRetrievalResult result = service.search(
                KnowledgeRetrievalContext.global(), "quy trinh the xe", List.of("PUBLIC"), 5);

        assertTrue(result.hasResults());
        assertEquals(documentId, result.results().getFirst().documentId());
        verify(retrievalPortOut, never()).hybridSearch(any(), any(), any(), anyString(), any(),
                any(int.class), any(int.class), any(double.class), any(double.class),
                any(int.class), any(double.class), any(double.class), any(int.class), any(int.class));
        verify(auditPortOut).save(any(RetrievalAudit.class));
    }

    @Test
    void scopeMismatchFallsBackToDatabase() {
        KnowledgeIndexVersion active = active();
        AiModelConfiguration configuration = embeddingConfiguration(active);
        when(indexVersionPortOut.findActive()).thenReturn(Optional.of(active));
        when(configurationPortOut.findById(any())).thenReturn(Optional.of(configuration));
        // Poisoned cache entry for another scope must be ignored.
        CachedRetrievalPayload poisoned = new CachedRetrievalPayload(1, Instant.now(),
                active.getIndexVersionId(), "checksum-1", "retrieval-policy-v1",
                "retrieval-threshold-v1", "wrong-scope", keyFactory.tenantFingerprint(null), 5,
                List.of(), 0.0, false, true, "INSUFFICIENT_EVIDENCE");
        when(retrievalCache.get(anyString())).thenReturn(Optional.of(poisoned));
        when(queryEmbeddingService.embedQuery(anyString(), anyString(), any(), any(int.class), anyString()))
                .thenReturn(EmbeddingResult.success(EmbeddingVector.of(new double[4], 4), "m"));
        when(retrievalPortOut.hybridSearch(any(), any(), any(), anyString(), any(),
                any(int.class), any(int.class), any(double.class), any(double.class),
                any(int.class), any(double.class), any(double.class), any(int.class), any(int.class)))
                .thenReturn(List.of());

        KnowledgeRetrievalResult result = service.search(
                KnowledgeRetrievalContext.global(), "quy trinh the xe", List.of("PUBLIC"), 5);

        assertEquals(KnowledgeRetrievalService.DIAG_INSUFFICIENT_EVIDENCE, result.diagnosticCode());
        verify(retrievalPortOut).hybridSearch(any(), any(), any(), anyString(), any(),
                any(int.class), any(int.class), any(double.class), any(double.class),
                any(int.class), any(double.class), any(double.class), any(int.class), any(int.class));
    }

    @Test
    void invalidPayloadFallsBackToDatabase() {
        KnowledgeIndexVersion active = active();
        AiModelConfiguration configuration = embeddingConfiguration(active);
        when(indexVersionPortOut.findActive()).thenReturn(Optional.of(active));
        when(configurationPortOut.findById(any())).thenReturn(Optional.of(configuration));
        when(retrievalCache.get(anyString())).thenThrow(new RuntimeException("redis down"));
        when(queryEmbeddingService.embedQuery(anyString(), anyString(), any(), any(int.class), anyString()))
                .thenReturn(EmbeddingResult.success(EmbeddingVector.of(new double[4], 4), "m"));
        HybridSearchRow row = new HybridSearchRow(UUID.randomUUID(), UUID.randomUUID(), "T", "C",
                null, 1, "S", 1, 1, BigDecimal.valueOf(0.9), BigDecimal.valueOf(0.9),
                BigDecimal.valueOf(0.05), 1);
        when(retrievalPortOut.hybridSearch(any(), any(), any(), anyString(), any(),
                any(int.class), any(int.class), any(double.class), any(double.class),
                any(int.class), any(double.class), any(double.class), any(int.class), any(int.class)))
                .thenReturn(List.of(row));

        KnowledgeRetrievalResult result = service.search(
                KnowledgeRetrievalContext.global(), "quy trinh the xe", List.of("PUBLIC"), 5);

        assertTrue(result.hasResults());
    }

    @Test
    void dbErrorIsNotNegativeCached() {
        KnowledgeIndexVersion active = active();
        AiModelConfiguration configuration = embeddingConfiguration(active);
        when(indexVersionPortOut.findActive()).thenReturn(Optional.of(active));
        when(configurationPortOut.findById(any())).thenReturn(Optional.of(configuration));
        when(retrievalCache.get(anyString())).thenReturn(Optional.empty());
        when(queryEmbeddingService.embedQuery(anyString(), anyString(), any(), any(int.class), anyString()))
                .thenReturn(EmbeddingResult.success(EmbeddingVector.of(new double[4], 4), "m"));
        when(retrievalPortOut.hybridSearch(any(), any(), any(), anyString(), any(),
                any(int.class), any(int.class), any(double.class), any(double.class),
                any(int.class), any(double.class), any(double.class), any(int.class), any(int.class)))
                .thenThrow(new RuntimeException("db down"));

        KnowledgeRetrievalResult result = service.search(
                KnowledgeRetrievalContext.global(), "quy trinh the xe", List.of("PUBLIC"), 5);

        assertEquals(KnowledgeRetrievalService.DIAG_RETRIEVAL_FAILED, result.diagnosticCode());
        verify(retrievalCache, never()).put(anyString(), any(), anyBoolean(), anyString());
    }
}
