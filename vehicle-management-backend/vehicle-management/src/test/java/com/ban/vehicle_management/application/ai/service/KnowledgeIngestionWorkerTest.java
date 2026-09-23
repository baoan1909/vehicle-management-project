package com.ban.vehicle_management.application.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.ai.port.out.AiModelConfigurationPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeChunkDraftPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeDocumentBlockPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeDocumentPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeDocumentStoragePort;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeExtractionPort;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeIngestionJobPortOut;
import com.ban.vehicle_management.application.ai.port.out.KnowledgeStagedEmbeddingPortOut;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeChunkDraft;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeChunkingResult;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeChunkSnapshot;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeDocument;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeDocumentBlock;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeExtractionResult;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeIngestionJob;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeStagedEmbedding;
import com.ban.vehicle_management.domain.ai.knowledge.policy.KnowledgeDocumentFilePolicy;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.EmbeddingFailure;
import com.ban.vehicle_management.domain.ai.model.EmbeddingResult;
import com.ban.vehicle_management.domain.ai.model.EmbeddingVector;
import com.ban.vehicle_management.application.ai.service.PiiRedactionService.RedactionResult;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import com.ban.vehicle_management.shared.enumeration.ai.AiUseCase;
import com.ban.vehicle_management.shared.enumeration.ai.IngestionStage;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeAccessScope;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeDocumentStatus;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIngestionJobStatus;
import java.io.ByteArrayInputStream;
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
class KnowledgeIngestionWorkerTest {

    @Mock
    private KnowledgeIngestionJobPortOut jobPortOut;
    @Mock
    private KnowledgeDocumentPortOut documentPortOut;
    @Mock
    private KnowledgeDocumentStoragePort storagePort;
    @Mock
    private KnowledgeExtractionPort extractionPort;
    @Mock
    private KnowledgeDocumentChunker chunker;
    @Mock
    private KnowledgeDocumentBlockPortOut blockPortOut;
    @Mock
    private KnowledgeChunkDraftPortOut chunkDraftPortOut;
    @Mock
    private KnowledgeStagedEmbeddingPortOut stagedEmbeddingPortOut;
    @Mock
    private AiModelConfigurationPortOut configurationPortOut;
    @Mock
    private EmbeddingService embeddingService;
    @Mock
    private EmbeddingPromptFormatter promptFormatter;
    @Mock
    private PiiRedactionService piiRedactionService;
    @Mock
    private KnowledgeIngestionProperties properties;

    private KnowledgeIngestionWorker worker;
    private KnowledgeIngestionJob job;
    private KnowledgeDocument document;
    private AiModelConfiguration configuration;
    private Instant now;
    private UUID documentId;
    private UUID jobId;
    private String workerId;

    @BeforeEach
    void setUp() {
        now = Instant.now();
        workerId = "worker-1";

        // Document
        document = KnowledgeDocument.newVersion(
                UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(), "Test Document",
                "obj-key", "test.pdf", "pdf", "application/pdf", 100L, "checksum", 1,
                KnowledgeAccessScope.PUBLIC, now);
        documentId = document.getDocumentId();

        // Job in claimed state - its document ID must match the generated domain ID.
        job = KnowledgeIngestionJob.create(documentId, 3, "idem-key", UUID.randomUUID(), now.minusSeconds(10));
        job.claim(workerId, 1, now, now.plus(Duration.ofMinutes(10)));
        jobId = job.getIngestionJobId();

        // Active embedding config
        configuration = new AiModelConfiguration();
        configuration.setConfigurationId(UUID.randomUUID());
        configuration.setProvider(AiProvider.GEMINI);
        configuration.setUseCase(AiUseCase.EMBEDDING);
        configuration.setModelId("gemini-embedding-2");
        configuration.setOutputDimension(768);
        configuration.setStatus(AiModelStatus.ACTIVE);

        // Worker with self-seam (self == this)
        worker = new KnowledgeIngestionWorker(
                jobPortOut, documentPortOut, storagePort, extractionPort, chunker,
                blockPortOut, chunkDraftPortOut, stagedEmbeddingPortOut,
                configurationPortOut, embeddingService, promptFormatter, piiRedactionService,
                properties, null);

        // Common lenient stubs
        lenient().when(jobPortOut.findById(jobId)).thenReturn(Optional.of(job));
        lenient().when(jobPortOut.findByIdForUpdate(jobId)).thenReturn(Optional.of(job));
        lenient().when(jobPortOut.renewLease(eq(jobId), eq(workerId), any(Instant.class)))
                .thenReturn(true);
        lenient().when(documentPortOut.findById(documentId)).thenReturn(Optional.of(document));
        lenient().when(storagePort.openRead(anyString())).thenReturn(new ByteArrayInputStream("content".getBytes()));
        lenient().when(configurationPortOut.findByUseCaseAndStatus(AiUseCase.EMBEDDING, AiModelStatus.ACTIVE))
                .thenReturn(List.of(configuration));
        lenient().when(promptFormatter.version()).thenReturn("prompt-v1");
        lenient().when(promptFormatter.formatDocument(anyString(), anyString(), anyString()))
                .thenReturn("embedding text");
        lenient().when(piiRedactionService.redact(anyString())).thenReturn(new RedactionResult("", false, false));
        lenient().when(properties.getLockDuration()).thenReturn(Duration.ofMinutes(10));
        lenient().when(properties.getRetryInitialDelay()).thenReturn(Duration.ofSeconds(10));
        lenient().when(properties.getMaxAttempts()).thenReturn(3);
    }

    @Test
    void processShouldFailDocumentMissingWhenDocumentAbsent() {
        when(documentPortOut.findById(documentId)).thenReturn(Optional.empty());

        worker.process(job);

        assertEquals(KnowledgeIngestionJobStatus.FAILED, job.getStatus());
        assertEquals("DOCUMENT_NOT_FOUND", job.getErrorCode());
        verify(jobPortOut, atLeastOnce()).save(job);
        verify(jobPortOut).appendEvent(any());
    }

    @Test
    void processShouldScheduleRetryForTransientEmbeddingFailure() {
        // Setup happy path until embedding
        KnowledgeExtractionResult extraction = new KnowledgeExtractionResult(
                "Title", List.of(new KnowledgeExtractionResult.ExtractedBlock("text", KnowledgeExtractionResult.BlockKind.PARAGRAPH, 1, "heading")), 1);
        when(extractionPort.extract(any(), anyString(), anyString())).thenReturn(extraction);

        KnowledgeChunkingResult chunking = new KnowledgeChunkingResult(
                List.of(),
                List.of(KnowledgeChunkDraft.of(documentId, 1, "title", "content", 1, "section", 0, "heading", 0, 1)));
        KnowledgeDocumentBlock block = KnowledgeDocumentBlock.create(
                documentId, 1, 0, KnowledgeDocumentBlock.BlockKind.PARAGRAPH,
                "heading", 1, "section", "text", now);
        when(chunker.createBlocks(eq(documentId), eq(1), any())).thenReturn(List.of(block));
        when(chunker.createChunks(eq(documentId), eq(1), anyString(), any())).thenReturn(chunking.chunks());

        UUID chunkId = UUID.randomUUID();
        when(chunkDraftPortOut.findByDocumentIdAndDocVersion(documentId, 1))
                .thenReturn(List.of(KnowledgeChunkSnapshot.of(chunkId, documentId, 1, "title", "content", null, 1, "section", 0, 1, "heading")));

        // First chunk embedding fails with transient EMBEDDING_RATE_LIMITED
        EmbeddingFailure failure = new EmbeddingFailure(
                "EMBEDDING_RATE_LIMITED", 429, "rate_limited", "rate limited", true, 60L);
        when(embeddingService.embed(any(), any()))
                .thenReturn(EmbeddingResult.failure(failure));

        worker.process(job);

        assertEquals(KnowledgeIngestionJobStatus.RETRYING, job.getStatus());
        assertNotNull(job.getNextAttemptAt());
        verify(jobPortOut, atLeastOnce()).save(job);
    }

    @Test
    void processShouldMarkFailedForNonTransientExtractionFailure() {
        // Extraction returns empty
        when(extractionPort.extract(any(), anyString(), anyString()))
                .thenReturn(new KnowledgeExtractionResult("Title", List.of(), 1));

        worker.process(job);

        assertEquals(KnowledgeIngestionJobStatus.FAILED, job.getStatus());
        assertEquals(KnowledgeDocumentFilePolicy.NO_EXTRACTABLE_TEXT, job.getErrorCode());
        verify(jobPortOut, atLeastOnce()).save(job);
    }

    @Test
    void fullPipelineShouldPersistReviewWhenAllStagesSucceed() {
        // Extraction success
        KnowledgeExtractionResult extraction = new KnowledgeExtractionResult(
                "Title", List.of(new KnowledgeExtractionResult.ExtractedBlock("text", KnowledgeExtractionResult.BlockKind.PARAGRAPH, 1, "heading")), 1);
        when(extractionPort.extract(any(), anyString(), anyString())).thenReturn(extraction);

        // Chunking success
        KnowledgeChunkDraft draft = KnowledgeChunkDraft.of(documentId, 1, "title", "content", 1, "section", 0, "heading", 0, 1);
        KnowledgeChunkingResult chunking = new KnowledgeChunkingResult(List.of(), List.of(draft));
        KnowledgeDocumentBlock block = KnowledgeDocumentBlock.create(
                documentId, 1, 0, KnowledgeDocumentBlock.BlockKind.PARAGRAPH,
                "heading", 1, "section", "text", now);
        when(chunker.createBlocks(eq(documentId), eq(1), any())).thenReturn(List.of(block));
        when(chunker.createChunks(eq(documentId), eq(1), anyString(), any())).thenReturn(chunking.chunks());

        UUID chunkId = UUID.randomUUID();
        when(chunkDraftPortOut.findByDocumentIdAndDocVersion(documentId, 1))
                .thenReturn(List.of(KnowledgeChunkSnapshot.of(chunkId, documentId, 1, "title", "content", null, 1, "section", 0, 1, "heading")));

        // Embedding success - vector must match outputDimension (768)
        double[] vectorValues = new double[768];
        vectorValues[0] = 0.1;
        vectorValues[1] = 0.2;
        EmbeddingVector vector = new EmbeddingVector(vectorValues, 768);
        when(embeddingService.embed(any(), any())).thenReturn(EmbeddingResult.success(vector, "gemini-embedding-2"));

        worker.process(job);

        assertEquals(KnowledgeIngestionJobStatus.REVIEW, job.getStatus());
        assertEquals(KnowledgeDocumentStatus.REVIEW, document.getStatus());
        verify(stagedEmbeddingPortOut).replaceForChunk(any(UUID.class), any());
        verify(jobPortOut, atLeastOnce()).appendEvent(any());
    }

    @Test
    void processShouldResumeAfterChunkingWithoutRepeatingExtractionOrChunking() {
        job.setLastCompletedStage(IngestionStage.CHUNKING);
        KnowledgeDocumentBlock persistedBlock = KnowledgeDocumentBlock.create(
                documentId, 1, 0, KnowledgeDocumentBlock.BlockKind.PARAGRAPH,
                "heading", 1, "section", "text", now);
        when(blockPortOut.findByDocumentIdAndDocVersion(documentId, 1))
                .thenReturn(List.of(persistedBlock));

        UUID chunkId = UUID.randomUUID();
        when(chunkDraftPortOut.findByDocumentIdAndDocVersion(documentId, 1))
                .thenReturn(List.of(KnowledgeChunkSnapshot.of(
                        chunkId, documentId, 1, "title", "content", null,
                        1, "section", 0, 1, "heading")));
        when(embeddingService.embed(any(), any()))
                .thenReturn(EmbeddingResult.success(new EmbeddingVector(new double[768], 768),
                        "gemini-embedding-2"));

        worker.process(job);

        assertEquals(KnowledgeIngestionJobStatus.REVIEW, job.getStatus());
        verify(storagePort, never()).openRead(anyString());
        verify(extractionPort, never()).extract(any(), anyString(), anyString());
        verify(chunker, never()).createBlocks(any(), anyInt(), any());
        verify(chunker, never()).createChunks(any(), anyInt(), anyString(), any());
        verify(stagedEmbeddingPortOut).replaceForChunk(eq(chunkId), any());
    }

    @Test
    void processShouldResumeAfterEmbeddingByEnteringReviewOnly() {
        job.setLastCompletedStage(IngestionStage.EMBEDDING);

        worker.process(job);

        assertEquals(KnowledgeIngestionJobStatus.REVIEW, job.getStatus());
        assertEquals(KnowledgeDocumentStatus.REVIEW, document.getStatus());
        verify(storagePort, never()).openRead(anyString());
        verify(extractionPort, never()).extract(any(), anyString(), anyString());
        verify(chunker, never()).createBlocks(any(), anyInt(), any());
        verify(chunker, never()).createChunks(any(), anyInt(), anyString(), any());
        verify(embeddingService, never()).embed(any(), any());
        verify(stagedEmbeddingPortOut, never()).replaceForChunk(any(), any());
    }

    @Test
    void processShouldStopWhenLeaseLostDuringExtraction() {
        // Renew lease fails at prepareDocument
        when(jobPortOut.renewLease(eq(jobId), eq(workerId), any(Instant.class))).thenReturn(false);

        worker.process(job);

        // No further stages executed, job unchanged
        verify(documentPortOut, never()).save(any());
        verify(extractionPort, never()).extract(any(), anyString(), anyString());
    }

    @Test
    void failOrRetryShouldScheduleRetryWhenTransientAndAttemptsRemaining() {
        // Directly test the self-proxied method
        UUID docId = UUID.randomUUID();
        KnowledgeDocument doc = KnowledgeDocument.newVersion(
                docId, UUID.randomUUID(), null, UUID.randomUUID(), "Doc", "k", "a.pdf", "pdf", "app/pdf", 1L, "c", 1, KnowledgeAccessScope.PUBLIC, now);
        when(documentPortOut.findById(docId)).thenReturn(Optional.of(doc));
        when(jobPortOut.findByIdForUpdate(jobId)).thenReturn(Optional.of(job));

        worker.failOrRetry(jobId, workerId, docId, "EMBEDDING_FAILED", "embedding error", true);

        assertEquals(KnowledgeIngestionJobStatus.RETRYING, job.getStatus());
        assertNotNull(job.getNextAttemptAt());
        verify(jobPortOut).save(job);
    }

    @Test
    void failOrRetryShouldMarkFailedWhenNonTransient() {
        when(jobPortOut.findByIdForUpdate(jobId)).thenReturn(Optional.of(job));

        worker.failOrRetry(jobId, workerId, documentId, "NO_EXTRACTABLE_TEXT", "no text", false);

        assertEquals(KnowledgeIngestionJobStatus.FAILED, job.getStatus());
        assertEquals("NO_EXTRACTABLE_TEXT", job.getErrorCode());
        verify(jobPortOut).save(job);
    }

    @Test
    void failOrRetryShouldMarkFailedWhenMaxAttemptsExceeded() {
        job.setAttemptCount(3); // equals maxAttempts
        when(jobPortOut.findByIdForUpdate(jobId)).thenReturn(Optional.of(job));

        worker.failOrRetry(jobId, workerId, documentId, "EMBEDDING_FAILED", "embedding error", true);

        assertEquals(KnowledgeIngestionJobStatus.FAILED, job.getStatus());
        verify(jobPortOut).save(job);
    }

    @Test
    void prepareDocumentShouldReturnFalseWhenLeaseLost() {
        when(jobPortOut.findById(jobId)).thenReturn(Optional.of(job));
        when(jobPortOut.renewLease(eq(jobId), eq(workerId), any(Instant.class))).thenReturn(false);

        boolean result = worker.prepareDocument(jobId, workerId, documentId);

        assertEquals(false, result);
    }

    @Test
    void persistChunksStageShouldReplaceChunksAndCompleteCheckpoint() {
        KnowledgeChunkDraft draft = KnowledgeChunkDraft.of(documentId, 1, "title", "content", 1, "section", 0, "heading", 0, 1);
        KnowledgeChunkingResult chunking = new KnowledgeChunkingResult(List.of(), List.of(draft));
        when(jobPortOut.findById(jobId)).thenReturn(Optional.of(job));
        when(jobPortOut.renewLease(eq(jobId), eq(workerId), any(Instant.class))).thenReturn(true);
        when(documentPortOut.findById(documentId)).thenReturn(Optional.of(document));

        boolean result = worker.persistChunksStage(jobId, workerId, documentId, chunking);

        assertEquals(true, result);
        verify(chunkDraftPortOut).replaceChunksForDocument(eq(documentId), eq(1), eq("chunker-v1"), any());
        verify(jobPortOut).save(job);
        assertEquals(IngestionStage.EMBEDDING, job.getCurrentStage());
        assertEquals(IngestionStage.CHUNKING, job.getLastCompletedStage());
    }
}
