package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.ai.port.out.AiModelConfigurationPortOut;
import com.ban.vehicle_management.application.ai.command.CreateKnowledgeIndexVersionCommand;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeChunkPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeEmbeddingPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeIndexVersionPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.KnowledgeIndexVersion;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import com.ban.vehicle_management.shared.enumeration.ai.AiUseCase;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIndexVersionStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KnowledgeIndexVersionUseCaseImplTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;
    @Mock
    private KnowledgeIndexVersionPortOut indexVersionPortOut;
    @Mock
    private AiModelConfigurationPortOut configurationPortOut;
    @Mock
    private KnowledgeChunkPortOut chunkPortOut;
    @Mock
    private KnowledgeEmbeddingPortOut embeddingPortOut;
    @Mock
    private EmbeddingPromptFormatter promptFormatter;

    private EmbeddingProperties embeddingProperties;

    private KnowledgeIndexVersionUseCaseImpl useCase;
    private final UUID actor = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        embeddingProperties = new EmbeddingProperties();
        embeddingProperties.setEnabled(true);
        useCase = new KnowledgeIndexVersionUseCaseImpl(
                currentAccountPortIn,
                indexVersionPortOut,
                configurationPortOut,
                chunkPortOut,
                embeddingPortOut,
                promptFormatter,
                embeddingProperties
        );
        lenient().when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(actor);
        lenient().when(promptFormatter.version()).thenReturn("rag-qa-v1");
        lenient().when(promptFormatter.supports(any())).thenReturn(true);
        lenient().when(chunkPortOut.calculateEligibleCorpusChecksum()).thenReturn("checksum");
        lenient().when(indexVersionPortOut.save(any(KnowledgeIndexVersion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createDraftShouldPersistDreamFromEmbeddingConfiguration() {
        AiModelConfiguration configuration = embeddingConfiguration(true);
        when(configurationPortOut.findById(configuration.getConfigurationId()))
                .thenReturn(Optional.of(configuration));

        KnowledgeIndexVersion draft = useCase.createDraft(command(configuration.getConfigurationId(), "IDX-MAIN-1"));

        assertEquals("IDX-MAIN-1", draft.getVersionCode());
        assertEquals(configuration.getConfigurationId(), draft.getModelConfigurationId());
        assertEquals(768, draft.getDimension());
        assertEquals(KnowledgeIndexVersionStatus.DRAFT, draft.getStatus());
        verify(indexVersionPortOut).save(any(KnowledgeIndexVersion.class));
    }

    @Test
    void createDraftShouldGenerateCodeWhenBlankProvided() {
        AiModelConfiguration configuration = embeddingConfiguration(true);
        when(configurationPortOut.findById(configuration.getConfigurationId()))
                .thenReturn(Optional.of(configuration));

        KnowledgeIndexVersion draft = useCase.createDraft(command(configuration.getConfigurationId(), " "));

        assertEquals(true, draft.getVersionCode().startsWith("IDX-"));
    }

    @Test
    void createDraftShouldRejectNonEmbeddingConfiguration() {
        AiModelConfiguration configuration = embeddingConfiguration(true);
        configuration.setUseCase(AiUseCase.SUPPORT_CHAT);
        when(configurationPortOut.findById(configuration.getConfigurationId()))
                .thenReturn(Optional.of(configuration));

        assertThrows(
                BadRequestException.class,
                () -> useCase.createDraft(command(configuration.getConfigurationId(), "IDX-1"))
        );
    }

    @Test
    void createDraftShouldRejectMissingConfiguration() {
        when(configurationPortOut.findById(any(UUID.class))).thenReturn(Optional.empty());
        assertThrows(
                NotFoundException.class,
                () -> useCase.createDraft(command(UUID.randomUUID(), "IDX-1"))
        );
    }

    @Test
    void startBuildShouldSnapshotExpectedCount() {
        KnowledgeIndexVersion draft = draftVersion(KnowledgeIndexVersionStatus.DRAFT);
        when(indexVersionPortOut.findByIdForUpdate(draft.getIndexVersionId()))
                .thenReturn(Optional.of(draft));
        when(chunkPortOut.countEligibleChunks()).thenReturn(14L);
        when(chunkPortOut.calculateEligibleCorpusChecksum()).thenReturn("corpus-checksum");
        when(configurationPortOut.findById(draft.getModelConfigurationId()))
                .thenReturn(Optional.of(embeddingConfiguration(true)));

        KnowledgeIndexVersion building = useCase.startBuild(draft.getIndexVersionId());

        assertEquals(KnowledgeIndexVersionStatus.BUILDING, building.getStatus());
        assertEquals(14, building.getExpectedChunkCount());
    }

    @Test
    void activateShouldPromoteReadyIndexAndRetireCurrent() {
        KnowledgeIndexVersion target = draftVersion(KnowledgeIndexVersionStatus.READY);
        KnowledgeIndexVersion current = draftVersion(KnowledgeIndexVersionStatus.ACTIVE);
        AiModelConfiguration configuration = embeddingConfiguration(true);
        when(indexVersionPortOut.findByIdForUpdate(target.getIndexVersionId()))
                .thenReturn(Optional.of(target));
        when(indexVersionPortOut.findActiveForUpdate()).thenReturn(Optional.of(current));
        when(configurationPortOut.findById(any(UUID.class)))
                .thenReturn(Optional.of(configuration));
        when(embeddingPortOut.countEmbeddedByIndexVersion(target.getIndexVersionId())).thenReturn(1L);
        when(embeddingPortOut.countInvalidVectors(target.getIndexVersionId())).thenReturn(0L);

        KnowledgeIndexVersion activated = useCase.activate(target.getIndexVersionId());

        assertEquals(KnowledgeIndexVersionStatus.ACTIVE, activated.getStatus());
        assertEquals(KnowledgeIndexVersionStatus.RETIRED, current.getStatus());
    }

    @Test
    void activateShouldRejectNotReadyIndex() {
        KnowledgeIndexVersion target = draftVersion(KnowledgeIndexVersionStatus.DRAFT);
        when(indexVersionPortOut.findByIdForUpdate(target.getIndexVersionId()))
                .thenReturn(Optional.of(target));

        assertThrows(
                BadRequestException.class,
                () -> useCase.activate(target.getIndexVersionId())
        );
    }

    @Test
    void rollbackShouldRejectWhenNoActiveIndexExists() {
        KnowledgeIndexVersion target = draftVersion(KnowledgeIndexVersionStatus.RETIRED);
        AiModelConfiguration configuration = embeddingConfiguration(true);
        when(indexVersionPortOut.findByIdForUpdate(target.getIndexVersionId()))
                .thenReturn(Optional.of(target));
        when(configurationPortOut.findById(any(UUID.class)))
                .thenReturn(Optional.of(configuration));
        when(embeddingPortOut.countEmbeddedByIndexVersion(target.getIndexVersionId())).thenReturn(1L);
        when(embeddingPortOut.countInvalidVectors(target.getIndexVersionId())).thenReturn(0L);
        when(indexVersionPortOut.findActiveForUpdate()).thenReturn(Optional.empty());

        assertThrows(
                BadRequestException.class,
                () -> useCase.rollback(target.getIndexVersionId())
        );
    }

    private KnowledgeIndexVersion draftVersion(KnowledgeIndexVersionStatus status) {
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
                actor,
                Instant.now()
        );
        version.setExpectedChunkCount(1);
        switch (status) {
            case BUILDING -> version.startBuild(actor, Instant.now());
            case READY -> {
                version.startBuild(actor, Instant.now());
                version.markReady(Instant.now());
            }
            case ACTIVE -> {
                version.startBuild(actor, Instant.now());
                version.markReady(Instant.now());
                version.activate(actor, Instant.now());
            }
            case RETIRED -> {
                version.startBuild(actor, Instant.now());
                version.markReady(Instant.now());
                version.activate(actor, Instant.now());
                version.retire(actor, Instant.now());
            }
            case FAILED -> {
                version.startBuild(actor, Instant.now());
                version.fail("EMBEDDING_FAILED", Instant.now());
            }
            default -> {
            }
        }
        return version;
    }

    private AiModelConfiguration embeddingConfiguration(boolean enabled) {
        AiModelConfiguration configuration = new AiModelConfiguration();
        configuration.setConfigurationId(UUID.randomUUID());
        configuration.setProvider(AiProvider.GEMINI);
        configuration.setUseCase(AiUseCase.EMBEDDING);
        configuration.setModelId("gemini-embedding-2");
        configuration.setApiVersion("v1beta");
        configuration.setOutputDimension(768);
        configuration.setStatus(enabled ? AiModelStatus.ACTIVE : AiModelStatus.DISABLED);
        return configuration;
    }

    private CreateKnowledgeIndexVersionCommand command(UUID configurationId, String versionCode) {
        return new CreateKnowledgeIndexVersionCommand(configurationId, versionCode);
    }
}
