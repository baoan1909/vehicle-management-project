package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.ai.port.out.AiModelConfigurationPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeIndexVersionPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeRetrievalAuditPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeRetrievalPortOut;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.EmbeddingResult;
import com.ban.vehicle_management.domain.ai.model.EmbeddingVector;
import com.ban.vehicle_management.domain.ai.model.HybridSearchRow;
import com.ban.vehicle_management.domain.ai.model.KnowledgeIndexVersion;
import com.ban.vehicle_management.domain.ai.model.KnowledgeRetrievalContext;
import com.ban.vehicle_management.domain.ai.model.KnowledgeRetrievalResult;
import com.ban.vehicle_management.domain.ai.model.RetrievalAudit;
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
import org.mockito.ArgumentCaptor;
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
    private KnowledgeRetrievalAuditPortOut auditPortOut;
    @Mock
    private EmbeddingService embeddingService;
    @Mock
    private EmbeddingPromptFormatter promptFormatter;
    @Mock
    private KnowledgeAccessContextResolver accessContextResolver;

    private EmbeddingProperties properties;
    private RetrievalProperties retrievalProperties;
    private KnowledgeRetrievalService service;
    private final PiiRedactionService piiRedactionService = new PiiRedactionService();

    @BeforeEach
    void setUp() {
        properties = new EmbeddingProperties();
        properties.setEnabled(true);
        properties.setVectorSearchEnabled(true);
        properties.setLexicalFallbackEnabled(true);
        retrievalProperties = new RetrievalProperties();
        service = new KnowledgeRetrievalService(
                retrievalPortOut,
                indexVersionPortOut,
                configurationPortOut,
                auditPortOut,
                embeddingService,
                promptFormatter,
                piiRedactionService,
                properties,
                retrievalProperties,
                accessContextResolver);
        lenient().when(promptFormatter.supports(anyString())).thenReturn(true);
        lenient().when(promptFormatter.formatQuery(anyString(), anyString())).thenReturn("nội dung đã định dạng");
        lenient().when(auditPortOut.save(any(RetrievalAudit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void searchShouldReturnFeatureDisabledWhenDisabled() {
        properties.setEnabled(false);

        KnowledgeRetrievalResult result = service.search(
                KnowledgeRetrievalContext.global(), "hỏi", List.of("PUBLIC"), 5);

        assertEquals(KnowledgeRetrievalService.DIAG_FEATURE_DISABLED, result.diagnosticCode());
        assertFalse(result.hasResults());
    }

    @Test
    void searchShouldAcceptTrustedTenantContext() {
        KnowledgeRetrievalResult result = service.search(
                KnowledgeRetrievalContext.forTenant(UUID.randomUUID()), "hỏi", List.of("PUBLIC"), 5);

        assertEquals(KnowledgeRetrievalService.DIAG_NO_ACTIVE_KNOWLEDGE_INDEX, result.diagnosticCode());
        assertFalse(result.hasResults());
        verify(embeddingService, never()).embed(any(), any());
    }

    @Test
    void searchShouldReturnNoActiveIndexWhenNoneExists() {
        when(indexVersionPortOut.findActive()).thenReturn(Optional.empty());

        KnowledgeRetrievalResult result = service.search(
                KnowledgeRetrievalContext.global(), "hỏi", List.of("PUBLIC"), 5);

        assertEquals(KnowledgeRetrievalService.DIAG_NO_ACTIVE_KNOWLEDGE_INDEX, result.diagnosticCode());
        assertNull(result.activeIndexVersionId());
    }

    @Test
    void searchShouldReturnConfigInvalidWhenEmbeddingConfigurationMissing() {
        KnowledgeIndexVersion active = activeIndexVersion();
        when(indexVersionPortOut.findActive()).thenReturn(Optional.of(active));
        when(configurationPortOut.findById(active.getModelConfigurationId())).thenReturn(Optional.empty());

        KnowledgeRetrievalResult result = service.search(
                KnowledgeRetrievalContext.global(), "hỏi", List.of("PUBLIC"), 5);

        assertEquals(KnowledgeRetrievalService.DIAG_INDEX_CONFIG_INVALID, result.diagnosticCode());
        verify(retrievalPortOut, never()).hybridSearch(any(), any(), any(), anyString(), anyList(), anyInt(), anyInt(),
                anyDouble(), anyDouble(), anyInt(), anyDouble(), anyDouble(), anyInt(), anyInt());
    }

    @Test
    void searchShouldFuseHybridBranchesAndPersistAudit() {
        KnowledgeIndexVersion active = activeIndexVersion();
        AiModelConfiguration configuration = embeddingConfiguration();
        HybridSearchRow vectorOnly = row(1, null, 0.9, null);
        HybridSearchRow lexicalOnly = row(null, 1, null, 0.8);
        HybridSearchRow both = row(2, 2, 0.7, 0.6);
        when(indexVersionPortOut.findActive()).thenReturn(Optional.of(active));
        when(configurationPortOut.findById(active.getModelConfigurationId())).thenReturn(Optional.of(configuration));
        when(embeddingService.embed(any(), any())).thenReturn(
                EmbeddingResult.success(EmbeddingVector.of(new double[768], 768), "gemini-embedding-2"));
        when(retrievalPortOut.hybridSearch(any(), any(), any(), anyString(), anyList(), anyInt(), anyInt(),
                anyDouble(), anyDouble(), anyInt(), anyDouble(), anyDouble(), anyInt(), anyInt()))
                .thenReturn(List.of(vectorOnly, lexicalOnly, both));

        KnowledgeRetrievalResult result = service.search(
                KnowledgeRetrievalContext.global(), " Quy  trình  ĐĂNG ký ", List.of("PUBLIC"), 5);

        assertNull(result.diagnosticCode());
        assertTrue(result.evidenceSufficient());
        assertEquals(active.getIndexVersionId(), result.activeIndexVersionId());
        assertEquals(3, result.results().size());
        assertEquals("quy trình đăng ký", result.normalizedQuery());
        assertEquals(3, result.citationLabels().size());
        assertTrue(result.groundedConfidence().doubleValue() > 0);
        ArgumentCaptor<RetrievalAudit> audit = ArgumentCaptor.forClass(RetrievalAudit.class);
        verify(auditPortOut).save(audit.capture());
        assertEquals(3, audit.getValue().results().size());
        assertEquals("retrieval-policy-v1", audit.getValue().retrievalPolicyVersion());
        assertTrue(audit.getValue().normalizedQueryHash().length() == 64);
    }

    @Test
    void searchShouldReturnInsufficientEvidenceWhenHybridIsEmpty() {
        KnowledgeIndexVersion active = activeIndexVersion();
        AiModelConfiguration configuration = embeddingConfiguration();
        when(indexVersionPortOut.findActive()).thenReturn(Optional.of(active));
        when(configurationPortOut.findById(active.getModelConfigurationId())).thenReturn(Optional.of(configuration));
        when(embeddingService.embed(any(), any())).thenReturn(
                EmbeddingResult.success(EmbeddingVector.of(new double[768], 768), "gemini-embedding-2"));
        when(retrievalPortOut.hybridSearch(any(), any(), any(), anyString(), anyList(), anyInt(), anyInt(),
                anyDouble(), anyDouble(), anyInt(), anyDouble(), anyDouble(), anyInt(), anyInt()))
                .thenReturn(List.of());

        KnowledgeRetrievalResult result = service.search(
                KnowledgeRetrievalContext.global(), "câu hỏi lạ", List.of("PUBLIC"), 5);

        assertEquals(KnowledgeRetrievalService.DIAG_INSUFFICIENT_EVIDENCE, result.diagnosticCode());
        assertFalse(result.evidenceSufficient());
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

    private HybridSearchRow row(Integer vectorRank, Integer lexicalRank, Double vectorScore, Double lexicalScore) {
        return new HybridSearchRow(
                UUID.randomUUID(), UUID.randomUUID(), "Tiêu đề", "Nội dung", "Tóm tắt", 1, "Mục 1",
                vectorRank, lexicalRank,
                vectorScore == null ? null : BigDecimal.valueOf(vectorScore),
                lexicalScore == null ? null : BigDecimal.valueOf(lexicalScore),
                BigDecimal.valueOf(0.05), 1);
    }
}
