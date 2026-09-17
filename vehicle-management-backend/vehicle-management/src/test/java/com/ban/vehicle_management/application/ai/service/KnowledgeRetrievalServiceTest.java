package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.ai.port.out.AiModelConfigurationPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeIndexVersionPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeRetrievalPortOut;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.EmbeddingResult;
import com.ban.vehicle_management.domain.ai.model.EmbeddingVector;
import com.ban.vehicle_management.domain.ai.model.KnowledgeIndexVersion;
import com.ban.vehicle_management.domain.ai.model.KnowledgeRetrievalResult;
import com.ban.vehicle_management.domain.ai.model.KnowledgeSearchResult;
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
class KnowledgeRetrievalServiceTest {

    @Mock
    private KnowledgeRetrievalPortOut retrievalPortOut;
    @Mock
    private KnowledgeIndexVersionPortOut indexVersionPortOut;
    @Mock
    private AiModelConfigurationPortOut configurationPortOut;
    @Mock
    private EmbeddingService embeddingService;
    @Mock
    private EmbeddingPromptFormatter promptFormatter;
    @Mock
    private KnowledgeAccessContextResolver accessContextResolver;

    private EmbeddingProperties properties;
    private KnowledgeRetrievalService service;
    private final PiiRedactionService piiRedactionService = new PiiRedactionService();

    @BeforeEach
    void setUp() {
        properties = new EmbeddingProperties();
        properties.setEnabled(true);
        properties.setVectorSearchEnabled(true);
        properties.setLexicalFallbackEnabled(true);
        service = new KnowledgeRetrievalService(
                retrievalPortOut,
                indexVersionPortOut,
                configurationPortOut,
                embeddingService,
                promptFormatter,
                piiRedactionService,
                properties,
                accessContextResolver);
        lenient().when(promptFormatter.supports(anyString())).thenReturn(true);
        lenient().when(promptFormatter.formatQuery(anyString(), anyString())).thenReturn("nội dung đã định dạng");
    }

    @Test
    void searchShouldReturnFeatureDisabledWhenDisabled() {
        properties.setEnabled(false);

        KnowledgeRetrievalResult result = service.search(null, "hỏi", List.of("PUBLIC"), 5);

        assertEquals(KnowledgeRetrievalService.DIAG_FEATURE_DISABLED, result.diagnosticCode());
        assertFalse(result.hasResults());
    }

    @Test
    void searchShouldReturnFeatureDisabledWhenVectorDisabled() {
        properties.setVectorSearchEnabled(false);

        KnowledgeRetrievalResult result = service.search(null, "hỏi", List.of("PUBLIC"), 5);

        assertEquals(KnowledgeRetrievalService.DIAG_FEATURE_DISABLED, result.diagnosticCode());
    }

    @Test
    void searchShouldReturnNoActiveIndexWhenNoneExists() {
        when(indexVersionPortOut.findActive()).thenReturn(Optional.empty());

        KnowledgeRetrievalResult result = service.search(null, "hỏi", List.of("PUBLIC"), 5);

        assertEquals(KnowledgeRetrievalService.DIAG_NO_ACTIVE_KNOWLEDGE_INDEX, result.diagnosticCode());
        assertNull(result.activeIndexVersionId());
    }

    @Test
    void searchShouldReturnConfigInvalidWhenEmbeddingConfigurationMissing() {
        KnowledgeIndexVersion active = activeIndexVersion();
        when(indexVersionPortOut.findActive()).thenReturn(Optional.of(active));
        when(configurationPortOut.findById(active.getModelConfigurationId())).thenReturn(Optional.empty());

        KnowledgeRetrievalResult result = service.search(null, "hỏi", List.of("PUBLIC"), 5);

        assertEquals(KnowledgeRetrievalService.DIAG_INDEX_CONFIG_INVALID, result.diagnosticCode());
        verify(retrievalPortOut, never()).searchVector(any(), any(), any(), any(), anyInt());
    }

    @Test
    void searchShouldReturnVectorResultsWhenEmbeddingSucceeds() {
        KnowledgeIndexVersion active = activeIndexVersion();
        AiModelConfiguration configuration = embeddingConfiguration();
        KnowledgeSearchResult hit = hit(active);
        when(indexVersionPortOut.findActive()).thenReturn(Optional.of(active));
        when(configurationPortOut.findById(active.getModelConfigurationId())).thenReturn(Optional.of(configuration));
        when(embeddingService.embed(any(), any())).thenReturn(
                EmbeddingResult.success(EmbeddingVector.of(new double[768], 768), "gemini-embedding-2"));
        when(retrievalPortOut.searchVector(any(), any(), any(), any(), anyInt())).thenReturn(List.of(hit));
        when(retrievalPortOut.search(any(), anyString(), any(), any(), anyInt())).thenReturn(List.of());

        KnowledgeRetrievalResult result = service.search(null, "hỏi", List.of("PUBLIC"), 5);

        assertNull(result.diagnosticCode());
        assertEquals(active.getIndexVersionId(), result.activeIndexVersionId());
        assertEquals(1, result.results().size());
    }

    @Test
    void searchShouldFallbackToLexicalAndKeepDiagnosticWhenEmbeddingFails() {
        KnowledgeIndexVersion active = activeIndexVersion();
        AiModelConfiguration configuration = embeddingConfiguration();
        KnowledgeSearchResult hit = hit(active);
        when(indexVersionPortOut.findActive()).thenReturn(Optional.of(active));
        when(configurationPortOut.findById(active.getModelConfigurationId())).thenReturn(Optional.of(configuration));
        when(embeddingService.embed(any(), any())).thenReturn(
                EmbeddingResult.failure(
                        new com.ban.vehicle_management.domain.ai.model.EmbeddingFailure(
                                "EMBED_FAILED", null, null, "provider unavailable", true, null)));
        when(retrievalPortOut.search(any(), anyString(), any(), any(), anyInt())).thenReturn(List.of(hit));

        KnowledgeRetrievalResult result = service.search(null, "hỏi", List.of("PUBLIC"), 5);

        assertNull(result.diagnosticCode());
        assertEquals(1, result.results().size());
    }

    private KnowledgeIndexVersion activeIndexVersion() {
        KnowledgeIndexVersion version = KnowledgeIndexVersion.draft(
                "IDX-1", UUID.randomUUID(), AiProvider.GEMINI, "gemini-embedding-2", 768,
                "chunker-v1", "rag-qa-v1", "COSINE", "NONE", "checksum", null, Instant.now());
        version.startBuild(null, Instant.now());
        version.markReady(Instant.now());
        version.setStatus(KnowledgeIndexVersionStatus.ACTIVE);
        return version;
    }

    private AiModelConfiguration embeddingConfiguration() {
        AiModelConfiguration configuration = new AiModelConfiguration();
        configuration.setConfigurationId(UUID.randomUUID());
        configuration.setProvider(AiProvider.GEMINI);
        configuration.setUseCase(AiUseCase.EMBEDDING);
        configuration.setModelId("gemini-embedding-2");
        configuration.setApiVersion("v1beta");
        configuration.setOutputDimension(768);
        configuration.setStatus(AiModelStatus.ACTIVE);
        return configuration;
    }

    private KnowledgeSearchResult hit(KnowledgeIndexVersion active) {
        return new KnowledgeSearchResult(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Tiêu đề",
                "Nội dung",
                "Tóm tắt",
                1,
                "Mục 1",
                BigDecimal.ONE);
    }
}