package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.ai.port.out.AiModelConfigurationPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeQualityMetricsPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeQualityMetricsPortOut.KnowledgeQualityMetrics;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import com.ban.vehicle_management.shared.enumeration.ai.AiUseCase;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KnowledgeQualityServiceTest {

    @Mock
    private KnowledgeQualityMetricsPortOut metricsPortOut;
    @Mock
    private AiModelConfigurationPortOut configurationPortOut;

    private KnowledgeQualityService service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeQualityService(metricsPortOut, configurationPortOut);
        when(configurationPortOut.findByUseCaseAndStatus(AiUseCase.EMBEDDING, AiModelStatus.ACTIVE))
                .thenReturn(List.of());
    }

    @Test
    void summarizeShouldExposeAggregatedStatusCounts() {
        when(metricsPortOut.snapshot(any(), any(), any())).thenReturn(metrics(
                Map.of("ACTIVE", 1L), Map.of("READY", 2L, "REVIEW", 1L),
                Map.of("READY", 1L), Map.of("ACTIVE", 1L, "DRAFT", 1L),
                0, 0, 0, 0, 0, List.of()));

        KnowledgeQualityService.KnowledgeQualityDashboard dashboard = service.summarize();

        assertEquals(1L, dashboard.sourcesByStatus().get("ACTIVE"));
        assertEquals(2L, dashboard.documentsByStatus().get("READY"));
        assertEquals(1L, dashboard.activeIndexCount());
        assertEquals(2L, dashboard.totalIndexVersionCount());
    }

    @Test
    void summarizeShouldWarnWhenNoActiveIndexAndEmbeddingConfigInactive() {
        when(metricsPortOut.snapshot(any(), any(), any())).thenReturn(metrics(
                Map.of(), Map.of(), Map.of(), Map.of("DRAFT", 1L),
                0, 0, 0, 0, 0, List.of()));

        KnowledgeQualityService.KnowledgeQualityDashboard dashboard = service.summarize();

        assertTrue(hasWarning(dashboard, "NO_ACTIVE_INDEX"));
        assertTrue(hasWarning(dashboard, "EMBEDDING_CONFIG_INACTIVE"));
    }

    @Test
    void summarizeShouldNotWarnEmbeddingConfigInactiveWhenActiveConfigExists() {
        when(metricsPortOut.snapshot(any(), any(), any())).thenReturn(metrics(
                Map.of(), Map.of(), Map.of(), Map.of("ACTIVE", 1L),
                0, 0, 0, 0, 0, List.of()));
        when(configurationPortOut.findByUseCaseAndStatus(AiUseCase.EMBEDDING, AiModelStatus.ACTIVE))
                .thenReturn(List.of(embeddingConfiguration()));

        KnowledgeQualityService.KnowledgeQualityDashboard dashboard = service.summarize();

        assertFalse(hasWarning(dashboard, "EMBEDDING_CONFIG_INACTIVE"));
    }

    @Test
    void summarizeShouldExposeOperationalWarningsFromAggregateMetrics() {
        when(metricsPortOut.snapshot(any(), any(), any())).thenReturn(metrics(
                Map.of(), Map.of(), Map.of("FAILED", 2L), Map.of("ACTIVE", 1L, "FAILED", 1L),
                3, 4, 5, 6, 1, List.of()));

        KnowledgeQualityService.KnowledgeQualityDashboard dashboard = service.summarize();

        assertTrue(hasWarning(dashboard, "STUCK_INGESTION_JOBS"));
        assertTrue(hasWarning(dashboard, "STALE_INDEX_CANDIDATES"));
        assertTrue(hasWarning(dashboard, "FAILED_INGESTION_JOBS"));
        assertTrue(hasWarning(dashboard, "EXPIRED_DOCUMENTS"));
        assertTrue(hasWarning(dashboard, "MISSING_EMBEDDINGS"));
        assertTrue(hasWarning(dashboard, "INDEX_BUILD_FAILED"));
        assertTrue(hasWarning(dashboard, "ACTIVE_INDEX_INCOMPLETE"));
    }

    @Test
    void summarizeShouldWarnWhenActiveIndexModelIsUnavailable() {
        UUID configurationId = UUID.randomUUID();
        when(metricsPortOut.snapshot(any(), any(), any())).thenReturn(metrics(
                Map.of(), Map.of(), Map.of(), Map.of("ACTIVE", 1L),
                0, 0, 0, 0, 0, List.of(configurationId)));
        when(configurationPortOut.findById(configurationId)).thenReturn(Optional.empty());

        KnowledgeQualityService.KnowledgeQualityDashboard dashboard = service.summarize();

        assertTrue(hasWarning(dashboard, "MODEL_CONFIGURATION_UNAVAILABLE"));
    }

    private KnowledgeQualityMetrics metrics(
            Map<String, Long> sources,
            Map<String, Long> documents,
            Map<String, Long> jobs,
            Map<String, Long> indexes,
            long stuck,
            long stale,
            long expired,
            long missing,
            long incomplete,
            List<UUID> activeConfigurations) {
        return new KnowledgeQualityMetrics(
                sources, documents, jobs, indexes,
                indexes.getOrDefault("ACTIVE", 0L),
                indexes.values().stream().mapToLong(Long::longValue).sum(),
                stuck, stale, expired, missing, incomplete, activeConfigurations);
    }

    private boolean hasWarning(KnowledgeQualityService.KnowledgeQualityDashboard dashboard, String code) {
        return dashboard.warnings().stream().anyMatch(warning -> code.equals(warning.code()));
    }

    private AiModelConfiguration embeddingConfiguration() {
        AiModelConfiguration configuration = new AiModelConfiguration();
        configuration.setConfigurationId(UUID.randomUUID());
        configuration.setProvider(AiProvider.GEMINI);
        configuration.setUseCase(AiUseCase.EMBEDDING);
        configuration.setModelId("gemini-embedding-2");
        configuration.setOutputDimension(768);
        configuration.setStatus(AiModelStatus.ACTIVE);
        return configuration;
    }
}
