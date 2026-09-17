package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.ai.port.out.AiModelConfigurationPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeChunkPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeIndexVersionPortOut;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.KnowledgeIndexVersion;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import com.ban.vehicle_management.shared.enumeration.ai.AiUseCase;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIndexVersionStatus;
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
class KnowledgeIndexBuildCoordinatorTest {

    @Mock
    private KnowledgeIndexVersionPortOut indexVersionPortOut;
    @Mock
    private AiModelConfigurationPortOut configurationPortOut;
    @Mock
    private KnowledgeChunkPortOut chunkPortOut;
    @Mock
    private EmbeddingPromptFormatter promptFormatter;

    private KnowledgeIndexBuildCoordinator coordinator;

    @BeforeEach
    void setUp() {
        coordinator = new KnowledgeIndexBuildCoordinator(
                indexVersionPortOut, configurationPortOut, chunkPortOut, promptFormatter);
        lenient().when(promptFormatter.version()).thenReturn("rag-qa-v1");
        lenient().when(indexVersionPortOut.save(any(KnowledgeIndexVersion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void ensureAutoCandidateShouldSkipWhenCandidateAlreadyPending() {
        when(indexVersionPortOut.findFirstPendingCandidate())
                .thenReturn(Optional.of(buildingCandidate("corpus")));

        coordinator.ensureAutoCandidate();

        verify(indexVersionPortOut, never()).save(any(KnowledgeIndexVersion.class));
    }

    @Test
    void ensureAutoCandidateShouldSkipWhenCandidateAlreadyPendingIsUpToDate() {
        KnowledgeIndexVersion pending = buildingCandidate("corpus");
        when(indexVersionPortOut.findFirstPendingCandidate()).thenReturn(Optional.of(pending));
        when(chunkPortOut.calculateEligibleCorpusChecksum()).thenReturn("corpus");

        coordinator.ensureAutoCandidate();

        verify(indexVersionPortOut, never()).save(any(KnowledgeIndexVersion.class));
    }

    @Test
    void ensureAutoCandidateShouldFailStaleBuildingCandidateThenRebuild() {
        KnowledgeIndexVersion pending = buildingCandidate("old-checksum");
        AiModelConfiguration configuration = embeddingConfiguration();
        when(indexVersionPortOut.findFirstPendingCandidate()).thenReturn(Optional.of(pending));
        when(configurationPortOut.findByUseCaseAndStatus(AiUseCase.EMBEDDING, AiModelStatus.ACTIVE))
                .thenReturn(List.of(configuration));
        when(chunkPortOut.countEligibleChunks()).thenReturn(4L);
        when(chunkPortOut.calculateEligibleCorpusChecksum()).thenReturn("new-checksum");

        coordinator.ensureAutoCandidate();

        assertEquals(KnowledgeIndexVersionStatus.FAILED, pending.getStatus());
        assertEquals("KNOWLEDGE_CORPUS_CHANGED", pending.getFailureCode());
        verify(indexVersionPortOut, times(2)).save(any(KnowledgeIndexVersion.class));
    }

    @Test
    void ensureAutoCandidateShouldSkipWhenNoActiveEmbeddingConfiguration() {
        when(indexVersionPortOut.findFirstPendingCandidate()).thenReturn(Optional.empty());
        when(configurationPortOut.findByUseCaseAndStatus(AiUseCase.EMBEDDING, AiModelStatus.ACTIVE))
                .thenReturn(List.of());

        coordinator.ensureAutoCandidate();

        verify(indexVersionPortOut, never()).save(any(KnowledgeIndexVersion.class));
    }

    @Test
    void ensureAutoCandidateShouldSkipWhenNoEligibleChunks() {
        AiModelConfiguration configuration = embeddingConfiguration();
        when(indexVersionPortOut.findFirstPendingCandidate()).thenReturn(Optional.empty());
        when(configurationPortOut.findByUseCaseAndStatus(AiUseCase.EMBEDDING, AiModelStatus.ACTIVE))
                .thenReturn(List.of(configuration));
        when(chunkPortOut.countEligibleChunks()).thenReturn(0L);

        coordinator.ensureAutoCandidate();

        verify(indexVersionPortOut, never()).save(any(KnowledgeIndexVersion.class));
    }

    @Test
    void ensureAutoCandidateShouldCreateBuildingCandidate() {
        AiModelConfiguration configuration = embeddingConfiguration();
        when(indexVersionPortOut.findFirstPendingCandidate()).thenReturn(Optional.empty());
        when(configurationPortOut.findByUseCaseAndStatus(AiUseCase.EMBEDDING, AiModelStatus.ACTIVE))
                .thenReturn(List.of(configuration));
        when(chunkPortOut.countEligibleChunks()).thenReturn(12L);
        when(chunkPortOut.calculateEligibleCorpusChecksum()).thenReturn("corpus");

        coordinator.ensureAutoCandidate();

        verify(indexVersionPortOut).save(any(KnowledgeIndexVersion.class));
    }

    @Test
    void onCorpusChangedShouldFailBuildingCandidateThenRebuild() {
        KnowledgeIndexVersion pending = buildingCandidate("old-checksum");
        AiModelConfiguration configuration = embeddingConfiguration();
        when(indexVersionPortOut.findFirstPendingCandidate())
                .thenAnswer(invocation -> pending.getStatus() == KnowledgeIndexVersionStatus.BUILDING
                        ? Optional.of(pending)
                        : Optional.empty());
        when(configurationPortOut.findByUseCaseAndStatus(AiUseCase.EMBEDDING, AiModelStatus.ACTIVE))
                .thenReturn(List.of(configuration));
        when(chunkPortOut.countEligibleChunks()).thenReturn(5L);
        when(chunkPortOut.calculateEligibleCorpusChecksum()).thenReturn("new-corpus");

        coordinator.onCorpusChanged();

        assertEquals(KnowledgeIndexVersionStatus.FAILED, pending.getStatus());
        assertEquals("KNOWLEDGE_CORPUS_CHANGED", pending.getFailureCode());
        verify(indexVersionPortOut, times(2)).save(any(KnowledgeIndexVersion.class));
    }

    private KnowledgeIndexVersion buildingCandidate(String contentChecksum) {
        KnowledgeIndexVersion pending = KnowledgeIndexVersion.draft(
                "auto-x", UUID.randomUUID(), AiProvider.GEMINI, "model", 768,
                "chunker-v1", "rag-qa-v1", "COSINE", "NONE", contentChecksum, null, Instant.now());
        pending.startBuild(null, Instant.now());
        return pending;
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