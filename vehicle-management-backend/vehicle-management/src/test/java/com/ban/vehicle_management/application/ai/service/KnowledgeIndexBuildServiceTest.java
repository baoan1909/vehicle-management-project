package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.ai.port.out.AiModelConfigurationPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeChunkPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeEmbeddingPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeIndexVersionPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeStagedEmbeddingPortOut;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.KnowledgeIndexVersion;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import com.ban.vehicle_management.shared.enumeration.ai.AiUseCase;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIndexVersionStatus;
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
class KnowledgeIndexBuildServiceTest {

    @Mock private KnowledgeIndexVersionPortOut indexVersionPortOut;
    @Mock private AiModelConfigurationPortOut configurationPortOut;
    @Mock private KnowledgeChunkPortOut chunkPortOut;
    @Mock private KnowledgeEmbeddingPortOut embeddingPortOut;
    @Mock private KnowledgeStagedEmbeddingPortOut stagedEmbeddingPortOut;
    @Mock private EmbeddingService embeddingService;
    @Mock private EmbeddingPromptFormatter promptFormatter;
    @Mock private PiiRedactionService piiRedactionService;

    private KnowledgeIndexBuildService service;

    @BeforeEach
    void setUp() {
        EmbeddingProperties properties = new EmbeddingProperties();
        properties.setBuildLeaseDuration(Duration.ofMinutes(5));
        service = new KnowledgeIndexBuildService(
                indexVersionPortOut,
                configurationPortOut,
                chunkPortOut,
                embeddingPortOut,
                stagedEmbeddingPortOut,
                embeddingService,
                promptFormatter,
                piiRedactionService,
                properties
        );
    }

    @Test
    void shouldSkipWhenAnotherWorkerOwnsLease() {
        UUID indexVersionId = UUID.randomUUID();
        when(indexVersionPortOut.tryAcquireBuildLease(eq(indexVersionId), any(UUID.class), any(Instant.class)))
                .thenReturn(false);

        service.process(indexVersionId);

        verify(indexVersionPortOut, never()).findById(indexVersionId);
        verify(indexVersionPortOut, never()).releaseBuildLease(eq(indexVersionId), any(UUID.class));
    }

    @Test
    void shouldMarkFullyEmbeddedStableCorpusReadyAndReleaseLease() {
        KnowledgeIndexVersion version = buildingVersion();
        AiModelConfiguration configuration = embeddingConfiguration(version.getModelConfigurationId());
        when(indexVersionPortOut.tryAcquireBuildLease(eq(version.getIndexVersionId()), any(UUID.class), any(Instant.class)))
                .thenReturn(true);
        when(indexVersionPortOut.findById(version.getIndexVersionId())).thenReturn(Optional.of(version));
        when(configurationPortOut.findById(version.getModelConfigurationId())).thenReturn(Optional.of(configuration));
        when(promptFormatter.supports("rag-qa-v1")).thenReturn(true);
        when(chunkPortOut.findEligibleChunks(version.getIndexVersionId(), 5)).thenReturn(List.of());
        when(embeddingPortOut.countEmbeddedByIndexVersion(version.getIndexVersionId())).thenReturn(1L);
        when(chunkPortOut.calculateEligibleCorpusChecksum()).thenReturn("checksum");
        when(indexVersionPortOut.save(version)).thenReturn(version);

        service.process(version.getIndexVersionId());

        assertEquals(KnowledgeIndexVersionStatus.READY, version.getStatus());
        verify(indexVersionPortOut).releaseBuildLease(eq(version.getIndexVersionId()), any(UUID.class));
    }

    private KnowledgeIndexVersion buildingVersion() {
        KnowledgeIndexVersion version = KnowledgeIndexVersion.draft(
                "IDX-TEST",
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
        version.setExpectedChunkCount(1);
        return version;
    }

    private AiModelConfiguration embeddingConfiguration(UUID configurationId) {
        AiModelConfiguration configuration = new AiModelConfiguration();
        configuration.setConfigurationId(configurationId);
        configuration.setProvider(AiProvider.GEMINI);
        configuration.setUseCase(AiUseCase.EMBEDDING);
        configuration.setModelId("gemini-embedding-2");
        configuration.setOutputDimension(768);
        configuration.setStatus(AiModelStatus.ACTIVE);
        return configuration;
    }
}
