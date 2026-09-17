package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.ai.command.ArchiveKnowledgeDocumentCommand;
import com.ban.vehicle_management.application.ai.command.PublishKnowledgeDocumentCommand;
import com.ban.vehicle_management.application.ai.command.ReindexKnowledgeDocumentCommand;
import com.ban.vehicle_management.application.ai.command.RejectKnowledgeDocumentCommand;
import com.ban.vehicle_management.application.ai.command.UploadKnowledgeDocumentCommand;
import com.ban.vehicle_management.application.ai.port.out.AiModelConfigurationPortOut;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeChunkDraftPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeDocumentBlockPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeDocumentPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeDocumentStoragePort;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeDocumentValidationPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeIngestionJobPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeSourcePortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeStagedEmbeddingPortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeDocument;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeIngestionJob;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeChunkSnapshot;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeSource;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeStagedEmbedding;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeAccessScope;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeChunkStatus;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeDocumentStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
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
class KnowledgeDocumentUseCaseImplTest {

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;
    @Mock
    private KnowledgeSourcePortOut sourcePortOut;
    @Mock
    private KnowledgeDocumentPortOut documentPortOut;
    @Mock
    private KnowledgeIngestionJobPortOut ingestionJobPortOut;
    @Mock
    private KnowledgeDocumentBlockPortOut blockPortOut;
    @Mock
    private KnowledgeChunkDraftPortOut chunkDraftPortOut;
    @Mock
    private KnowledgeStagedEmbeddingPortOut stagedEmbeddingPortOut;
    @Mock
    private KnowledgeDocumentStoragePort storagePort;
    @Mock
    private KnowledgeDocumentValidationPortOut validationPort;
    @Mock
    private KnowledgeIndexBuildCoordinator indexBuildCoordinator;
    @Mock
    private AiModelConfigurationPortOut configurationPortOut;
    @Mock
    private EmbeddingPromptFormatter promptFormatter;

    private KnowledgeIngestionProperties properties;
    private KnowledgeDocumentUseCaseImpl useCase;
    private final UUID actor = UUID.randomUUID();
    private final UUID chunkId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        properties = new KnowledgeIngestionProperties();
        useCase = new KnowledgeDocumentUseCaseImpl(
                currentAccountPortIn,
                sourcePortOut,
                documentPortOut,
                ingestionJobPortOut,
                blockPortOut,
                chunkDraftPortOut,
                stagedEmbeddingPortOut,
                storagePort,
                validationPort,
                properties,
                indexBuildCoordinator,
                configurationPortOut,
                promptFormatter);
        lenient().when(promptFormatter.version()).thenReturn("prompt-v1");
        lenient().when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(actor);
        lenient().when(storagePort.store(any(byte[].class), anyString(), any(UUID.class), anyString()))
                .thenReturn("knowledge-object-key");
        lenient().when(configurationPortOut.findByUseCaseAndStatus(any(), any()))
                .thenReturn(List.of());
    }

    @Test
    void uploadDocumentShouldCreateDocAndScheduleJob() {
        KnowledgeSource source = activeSource();
        when(sourcePortOut.findById(source.getSourceId())).thenReturn(Optional.of(source));
        when(documentPortOut.findBySourceIdAndChecksum(eq(source.getSourceId()), anyString()))
                .thenReturn(Optional.empty());
        when(documentPortOut.insertIdempotent(any(KnowledgeDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase.uploadDocument(command(source.getSourceId(), "guide.pdf", "body"));

        assertTrue(result.created());
        assertEquals("guide.pdf", result.document().getOriginalFilename());
        assertEquals(KnowledgeDocumentStatus.PENDING, result.document().getStatus());
        verify(documentPortOut).insertIdempotent(any(KnowledgeDocument.class));
        verify(ingestionJobPortOut).save(any());
        verify(ingestionJobPortOut).appendEvent(any());
    }

    @Test
    void uploadDocumentShouldReturnDuplicateWhenChecksumMatches() {
        KnowledgeSource source = activeSource();
        KnowledgeDocument existing = KnowledgeDocument.newVersion(
                source.getSourceId(), UUID.randomUUID(), null, source.getTenantId(), "Cũ",
                "old-key", "guide.pdf", "pdf", "application/pdf", 1L, "sha", 1,
                KnowledgeAccessScope.PUBLIC, Instant.now());
        existing.setStatus(KnowledgeDocumentStatus.READY);
        when(sourcePortOut.findById(source.getSourceId())).thenReturn(Optional.of(source));
        when(documentPortOut.findBySourceIdAndChecksum(eq(source.getSourceId()), anyString()))
                .thenReturn(Optional.of(existing));

        var result = useCase.uploadDocument(command(source.getSourceId(), "guide.pdf", "body"));

        assertFalse(result.created());
        assertEquals(existing.getDocumentId(), result.document().getDocumentId());
        verify(storagePort, never()).store(any(byte[].class), anyString(), any(UUID.class), anyString());
        verify(ingestionJobPortOut, never()).save(any());
    }

    @Test
    void uploadDocumentShouldRejectUnsupportedExtension() {
        KnowledgeSource source = activeSource();
        when(sourcePortOut.findById(source.getSourceId())).thenReturn(Optional.of(source));

        assertThrows(
                BadRequestException.class,
                () -> useCase.uploadDocument(command(source.getSourceId(), "memo.exe", "MZ..."))
        );
    }

    @Test
    void uploadDocumentShouldRejectReusedIdempotencyKeyForDifferentContent() {
        KnowledgeSource source = activeSource();
        KnowledgeDocument previous = pendingDocument();
        KnowledgeIngestionJob previousJob = KnowledgeIngestionJob.create(
                previous.getDocumentId(), 3, "upload-idempotency-key", actor, Instant.now());
        when(sourcePortOut.findById(source.getSourceId())).thenReturn(Optional.of(source));
        when(ingestionJobPortOut.findByRequestedByAndIdempotencyKey(actor, "upload-idempotency-key"))
                .thenReturn(Optional.of(previousJob));
        when(documentPortOut.findById(previous.getDocumentId())).thenReturn(Optional.of(previous));

        assertThrows(ConflictException.class,
                () -> useCase.uploadDocument(command(source.getSourceId(), "guide.pdf", "different")));

        verify(storagePort, never()).store(any(byte[].class), anyString(), any(UUID.class), anyString());
    }

    @Test
    void reindexDocumentShouldRejectReusedIdempotencyKeyForAnotherDocument() {
        KnowledgeDocument requested = failedDocument();
        KnowledgeDocument other = failedDocument();
        other.setDocumentVersion(2);
        KnowledgeIngestionJob previousJob = KnowledgeIngestionJob.create(
                other.getDocumentId(), 3, "reindex-idempotency-key", actor, Instant.now());
        when(ingestionJobPortOut.findByRequestedByAndIdempotencyKey(actor, "reindex-idempotency-key"))
                .thenReturn(Optional.of(previousJob));
        when(documentPortOut.findById(requested.getDocumentId())).thenReturn(Optional.of(requested));
        when(documentPortOut.findById(other.getDocumentId())).thenReturn(Optional.of(other));

        assertThrows(ConflictException.class,
                () -> useCase.reindexDocument(new ReindexKnowledgeDocumentCommand(
                        requested.getDocumentId(), "reindex-idempotency-key")));
    }

    @Test
    void publishDocumentShouldApproveReviewDocument() {
        KnowledgeDocument document = reviewDocument();
        when(documentPortOut.findByIdForUpdate(document.getDocumentId())).thenReturn(Optional.of(document));
        when(documentPortOut.save(any(KnowledgeDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(chunkDraftPortOut.findByDocumentIdAndDocVersion(document.getDocumentId(), 1))
                .thenReturn(List.of(chunkSnapshot(document.getDocumentId())));
        when(stagedEmbeddingPortOut.findByChunkIds(anyList())).thenReturn(List.of(stagedEmbedding()));
        when(ingestionJobPortOut.findOpenByDocumentId(document.getDocumentId())).thenReturn(List.of());
        when(configurationPortOut.findByUseCaseAndStatus(any(), any()))
                .thenReturn(List.of(activeEmbeddingConfiguration()));

        KnowledgeDocument published = useCase.publishDocument(new PublishKnowledgeDocumentCommand(document.getDocumentId()));

        assertEquals(KnowledgeDocumentStatus.READY, published.getStatus());
        verify(chunkDraftPortOut).updateStatusByDocumentId(
                document.getDocumentId(), KnowledgeChunkStatus.DRAFT, KnowledgeChunkStatus.READY);
        verify(indexBuildCoordinator).ensureAutoCandidate();
    }

    @Test
    void publishDocumentShouldRejectWhenChunksLackStagedEmbeddings() {
        KnowledgeDocument document = reviewDocument();
        when(documentPortOut.findByIdForUpdate(document.getDocumentId())).thenReturn(Optional.of(document));
        when(chunkDraftPortOut.findByDocumentIdAndDocVersion(document.getDocumentId(), 1))
                .thenReturn(List.of(chunkSnapshot(document.getDocumentId())));
        when(stagedEmbeddingPortOut.findByChunkIds(anyList())).thenReturn(List.of());
        when(configurationPortOut.findByUseCaseAndStatus(any(), any()))
                .thenReturn(List.of(activeEmbeddingConfiguration()));

        assertThrows(
                BadRequestException.class,
                () -> useCase.publishDocument(new PublishKnowledgeDocumentCommand(document.getDocumentId()))
        );
        verify(chunkDraftPortOut, never()).updateStatusByDocumentId(
                any(), any(), any());
        verify(indexBuildCoordinator, never()).ensureAutoCandidate();
    }

    @Test
    void publishSupersedingVersionShouldNotArchiveOldVersionChunks() {
        UUID oldVersionId = UUID.randomUUID();
        KnowledgeDocument document = reviewDocument();
        document.setSupersedesDocumentId(oldVersionId);
        when(documentPortOut.findByIdForUpdate(document.getDocumentId())).thenReturn(Optional.of(document));
        when(documentPortOut.save(any(KnowledgeDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(chunkDraftPortOut.findByDocumentIdAndDocVersion(document.getDocumentId(), 1))
                .thenReturn(List.of(chunkSnapshot(document.getDocumentId())));
        when(stagedEmbeddingPortOut.findByChunkIds(anyList())).thenReturn(List.of(stagedEmbedding()));
        when(ingestionJobPortOut.findOpenByDocumentId(document.getDocumentId())).thenReturn(List.of());
        when(configurationPortOut.findByUseCaseAndStatus(any(), any()))
                .thenReturn(List.of(activeEmbeddingConfiguration()));

        useCase.publishDocument(new PublishKnowledgeDocumentCommand(document.getDocumentId()));

        verify(chunkDraftPortOut).updateStatusByDocumentId(
                document.getDocumentId(), KnowledgeChunkStatus.DRAFT, KnowledgeChunkStatus.READY);
        // Old version chunks are NOT archived - they remain READY in the ACTIVE index.
        // The next index build will only include the latest version per document_key.
        verify(chunkDraftPortOut, never()).updateStatusByDocumentId(
                eq(oldVersionId), eq(KnowledgeChunkStatus.READY), eq(KnowledgeChunkStatus.ARCHIVED));
    }

    @Test
    void publishDocumentShouldRejectPendingDocument() {
        KnowledgeDocument document = pendingDocument();
        when(documentPortOut.findByIdForUpdate(document.getDocumentId())).thenReturn(Optional.of(document));

        assertThrows(
                BadRequestException.class,
                () -> useCase.publishDocument(new PublishKnowledgeDocumentCommand(document.getDocumentId()))
        );
        verify(indexBuildCoordinator, never()).ensureAutoCandidate();
    }

    @Test
    void rejectDocumentShouldCleanUpAndFailJob() {
        KnowledgeDocument document = reviewDocument();
        when(documentPortOut.findByIdForUpdate(document.getDocumentId())).thenReturn(Optional.of(document));
        when(documentPortOut.save(any(KnowledgeDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(chunkDraftPortOut.findByDocumentIdAndDocVersion(document.getDocumentId(), 1))
                .thenReturn(List.of());
        when(ingestionJobPortOut.findLatestByDocumentId(document.getDocumentId())).thenReturn(Optional.empty());

        KnowledgeDocument rejected = useCase.rejectDocument(new RejectKnowledgeDocumentCommand(document.getDocumentId()));

        assertEquals(KnowledgeDocumentStatus.FAILED, rejected.getStatus());
        verify(chunkDraftPortOut).replaceChunksForDocument(
                eq(document.getDocumentId()), anyInt(), eq("CLEANUP"), anyList());
    }

    @Test
    void archiveDocumentShouldArchiveAndSignalCorpusChange() {
        KnowledgeDocument document = readyDocument();
        when(documentPortOut.findByIdForUpdate(document.getDocumentId())).thenReturn(Optional.of(document));
        when(documentPortOut.save(any(KnowledgeDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(ingestionJobPortOut.findLatestByDocumentId(document.getDocumentId())).thenReturn(Optional.empty());

        KnowledgeDocument archived = useCase.archiveDocument(new ArchiveKnowledgeDocumentCommand(document.getDocumentId()));

        assertEquals(KnowledgeDocumentStatus.ARCHIVED, archived.getStatus());
        verify(indexBuildCoordinator).onCorpusChanged();
    }

    @Test
    void reindexDocumentShouldCreateNewImmutableVersion() {
        KnowledgeDocument document = failedDocument();
        // Mock the new locking by document_key behavior
        KnowledgeDocumentPortOut.LockedDocumentVersion lockedVersion = new KnowledgeDocumentPortOut.LockedDocumentVersion(
                document.getDocumentId(), 1, KnowledgeDocumentStatus.FAILED);
        when(documentPortOut.lockLatestVersionByDocumentKey(document.getDocumentKey())).thenReturn(Optional.of(lockedVersion));
        when(documentPortOut.findByIdForUpdate(document.getDocumentId())).thenReturn(Optional.of(document));
        when(documentPortOut.insertIdempotent(any(KnowledgeDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        KnowledgeDocument reindexed = useCase.reindexDocument(
                new ReindexKnowledgeDocumentCommand(document.getDocumentId(), "reindex-idempotency-key"));

        assertEquals(KnowledgeDocumentStatus.PENDING, reindexed.getStatus());
        assertEquals(2, reindexed.getDocumentVersion());
        assertEquals(document.getDocumentKey(), reindexed.getDocumentKey());
        assertEquals(document.getDocumentId(), reindexed.getSupersedesDocumentId());
        verify(documentPortOut).lockLatestVersionByDocumentKey(document.getDocumentKey());
        verify(documentPortOut).insertIdempotent(any(KnowledgeDocument.class));
        verify(documentPortOut, never()).save(eq(document));
        verify(ingestionJobPortOut).save(any());
        verify(ingestionJobPortOut).appendEvent(any());
        verify(indexBuildCoordinator, never()).onCorpusChanged();
    }

    @Test
    void listDocumentsShouldReturnAll() {
        KnowledgeDocument first = pendingDocument();
        KnowledgeDocument second = reviewDocument();
        when(documentPortOut.findAll()).thenReturn(List.of(first, second));

        List<KnowledgeDocument> documents = useCase.listDocuments();

        assertEquals(2, documents.size());
        verify(documentPortOut).findAll();
    }

    private KnowledgeSource activeSource() {
        return KnowledgeSource.create(
                UUID.randomUUID(), "Sổ tay", null, KnowledgeAccessScope.PUBLIC, actor, Instant.now());
    }

    private UploadKnowledgeDocumentCommand command(UUID sourceId, String filename, String body) {
        return new UploadKnowledgeDocumentCommand(
                sourceId, null, filename, "application/octet-stream", body.getBytes(), "upload-idempotency-key");
    }

    private KnowledgeDocument pendingDocument() {
        return KnowledgeDocument.newVersion(
                UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(), "Tiêu đề",
                "k", "guide.pdf", "pdf", "application/pdf", 1024L, "sha", 1,
                KnowledgeAccessScope.PUBLIC, Instant.now());
    }

    private KnowledgeDocument reviewDocument() {
        KnowledgeDocument document = pendingDocument();
        document.markProcessing(Instant.now());
        document.markReview(Instant.now());
        return document;
    }

    private KnowledgeDocument readyDocument() {
        KnowledgeDocument document = pendingDocument();
        document.approveForPublish(actor, Instant.now());
        return document;
    }

    private KnowledgeDocument failedDocument() {
        KnowledgeDocument document = pendingDocument();
        document.fail("EXTRACTION_FAILED", Instant.now());
        return document;
    }

    private KnowledgeChunkSnapshot chunkSnapshot(UUID documentId) {
        return KnowledgeChunkSnapshot.of(
                chunkId, documentId, 1, "Chunk", "Nội dung", "Tóm tắt",
                1, "1.1", 0, 10, "Heading");
    }

    private UUID configId = UUID.randomUUID();
    // Pre-computed SHA-256 of "Nội dung" (the chunk content used in chunkSnapshot)
    private static final String CONTENT_HASH = "75931e76c230c525f1aae50c3b952697de2c1e2f6f1e5af14481a65734a7363d";

    private KnowledgeStagedEmbedding stagedEmbedding() {
        KnowledgeStagedEmbedding staged = new KnowledgeStagedEmbedding();
        staged.setChunkId(chunkId);
        // Use the same configuration ID as the active configuration
        // Create a valid embedding vector with correct dimension
        double[] values = new double[768];
        for (int i = 0; i < 768; i++) {
            values[i] = 0.1;
        }
        staged.setEmbedding(new com.ban.vehicle_management.domain.ai.model.EmbeddingVector(values, 768));
        staged.setProvider(com.ban.vehicle_management.shared.enumeration.ai.AiProvider.GEMINI);
        staged.setModelConfigurationId(configId);
        staged.setModelId("gemini-embedding-2");
        staged.setEmbeddingDimension(768);
        staged.setEmbeddingPromptVersion("prompt-v1");
        staged.setChunkerVersion("chunker-v1");
        staged.setContentHash(CONTENT_HASH);
        staged.setEmbeddingFingerprint(KnowledgeStagedEmbedding.fingerprint(
                CONTENT_HASH, "gemini-embedding-2", "prompt-v1", "chunker-v1"));
        return staged;
    }

    private AiModelConfiguration activeEmbeddingConfiguration() {
        AiModelConfiguration configuration = new AiModelConfiguration();
        configuration.setConfigurationId(configId);
        configuration.setProvider(com.ban.vehicle_management.shared.enumeration.ai.AiProvider.GEMINI);
        configuration.setUseCase(com.ban.vehicle_management.shared.enumeration.ai.AiUseCase.EMBEDDING);
        configuration.setModelId("gemini-embedding-2");
        configuration.setOutputDimension(768);
        configuration.setStatus(com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus.ACTIVE);
        return configuration;
    }
}
