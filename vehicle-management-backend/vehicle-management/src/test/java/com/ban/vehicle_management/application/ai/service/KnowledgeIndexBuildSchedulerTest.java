package com.ban.vehicle_management.application.ai.service;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.ai.port.out.KnowledgeIndexVersionPortOut;
import com.ban.vehicle_management.domain.ai.model.KnowledgeIndexVersion;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIndexVersionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KnowledgeIndexBuildSchedulerTest {

    @Mock
    private EmbeddingProperties properties;
    @Mock
    private KnowledgeIndexVersionPortOut indexVersionPortOut;
    @Mock
    private KnowledgeIndexBuildService buildService;

    @Test
    void shouldSkipProcessingWhenFeatureDisabled() {
        when(properties.isEnabled()).thenReturn(false);
        KnowledgeIndexBuildScheduler scheduler = new KnowledgeIndexBuildScheduler(
                properties, indexVersionPortOut, buildService
        );

        scheduler.buildPendingIndexVersions();

        verify(indexVersionPortOut, never()).findByStatus(KnowledgeIndexVersionStatus.BUILDING);
        verify(buildService, never()).process(org.mockito.ArgumentMatchers.any(UUID.class));
    }

    @Test
    void shouldProcessBuildingVersions() {
        when(properties.isEnabled()).thenReturn(true);
        KnowledgeIndexVersion versionA = buildingVersion();
        KnowledgeIndexVersion versionB = buildingVersion();
        when(indexVersionPortOut.findByStatus(KnowledgeIndexVersionStatus.BUILDING))
                .thenReturn(List.of(versionA, versionB));

        KnowledgeIndexBuildScheduler scheduler = new KnowledgeIndexBuildScheduler(
                properties, indexVersionPortOut, buildService
        );
        scheduler.buildPendingIndexVersions();

        verify(buildService).process(versionA.getIndexVersionId());
        verify(buildService).process(versionB.getIndexVersionId());
    }

    @Test
    void shouldNotAbortWhenOneVersionFails() {
        when(properties.isEnabled()).thenReturn(true);
        KnowledgeIndexVersion versionA = buildingVersion();
        KnowledgeIndexVersion versionB = buildingVersion();
        when(indexVersionPortOut.findByStatus(KnowledgeIndexVersionStatus.BUILDING))
                .thenReturn(List.of(versionA, versionB));
        doThrow(new RuntimeException("boom")).when(buildService).process(versionA.getIndexVersionId());

        KnowledgeIndexBuildScheduler scheduler = new KnowledgeIndexBuildScheduler(
                properties, indexVersionPortOut, buildService
        );
        scheduler.buildPendingIndexVersions();

        verify(buildService).process(versionB.getIndexVersionId());
    }

    private KnowledgeIndexVersion buildingVersion() {
        KnowledgeIndexVersion version = KnowledgeIndexVersion.draft(
                "IDX-" + UUID.randomUUID(),
                UUID.randomUUID(),
                AiProvider.GEMINI,
                "gemini-embedding-2",
                768,
                "chunker-v1",
                "rag-qa-v1",
                "COSINE",
                "NONE",
                "checksum",
                UUID.randomUUID(),
                Instant.now()
        );
        version.startBuild(UUID.randomUUID(), Instant.now());
        return version;
    }
}
