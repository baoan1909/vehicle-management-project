package com.ban.vehicle_management.application.ai.service;

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
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeIngestionJobEvent;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeStagedEmbedding;
import com.ban.vehicle_management.domain.ai.knowledge.policy.KnowledgeDocumentFilePolicy;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.EmbeddingRequest;
import com.ban.vehicle_management.domain.ai.model.EmbeddingResult;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiUseCase;
import com.ban.vehicle_management.shared.enumeration.ai.IngestionStage;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processes one claimed ingestion job through extract -> chunk -> stage-embed.
 *
 * Cleanup: claiming is a short transaction (orchestrator), and each pipeline stage is
 * persisted in its own short transaction via the self proxy. The expensive engine calls
 * (PDF/DOCX/HTML extraction, chunking, embedding) run OUTSIDE any database transaction so
 * a slow provider never pins a DB connection. Every stage first validates the worker lease
 * (renewLease) and stops silently when the job was re-claimed by another worker. Retries
 * distinguish transient failures (backoff via EmbeddingRetryPolicy) from permanent ones.
 */
@Component
public class KnowledgeIngestionWorker {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIngestionWorker.class);


    private final KnowledgeIngestionJobPortOut jobPortOut;
    private final KnowledgeDocumentPortOut documentPortOut;
    private final KnowledgeDocumentStoragePort storagePort;
    private final KnowledgeExtractionPort extractionPort;
    private final KnowledgeDocumentChunker chunker;
    private final KnowledgeDocumentBlockPortOut blockPortOut;
    private final KnowledgeChunkDraftPortOut chunkDraftPortOut;
    private final KnowledgeStagedEmbeddingPortOut stagedEmbeddingPortOut;
    private final AiModelConfigurationPortOut configurationPortOut;
    private final EmbeddingService embeddingService;
    private final EmbeddingPromptFormatter promptFormatter;
    private final PiiRedactionService piiRedactionService;
    private final KnowledgeIngestionProperties properties;
    private final EmbeddingRetryPolicy retryPolicy;

    private final KnowledgeIngestionWorker self;

    public KnowledgeIngestionWorker(
            KnowledgeIngestionJobPortOut jobPortOut,
            KnowledgeDocumentPortOut documentPortOut,
            KnowledgeDocumentStoragePort storagePort,
            KnowledgeExtractionPort extractionPort,
            KnowledgeDocumentChunker chunker,
            KnowledgeDocumentBlockPortOut blockPortOut,
            KnowledgeChunkDraftPortOut chunkDraftPortOut,
            KnowledgeStagedEmbeddingPortOut stagedEmbeddingPortOut,
            AiModelConfigurationPortOut configurationPortOut,
            EmbeddingService embeddingService,
            EmbeddingPromptFormatter promptFormatter,
            PiiRedactionService piiRedactionService,
            KnowledgeIngestionProperties properties,
            @Lazy KnowledgeIngestionWorker self) {
        this.jobPortOut = jobPortOut;
        this.documentPortOut = documentPortOut;
        this.storagePort = storagePort;
        this.extractionPort = extractionPort;
        this.chunker = chunker;
        this.blockPortOut = blockPortOut;
        this.chunkDraftPortOut = chunkDraftPortOut;
        this.stagedEmbeddingPortOut = stagedEmbeddingPortOut;
        this.configurationPortOut = configurationPortOut;
        this.embeddingService = embeddingService;
        this.promptFormatter = promptFormatter;
        this.piiRedactionService = piiRedactionService;
        this.properties = properties;
        this.retryPolicy = new EmbeddingRetryPolicy(
                properties.getRetryInitialDelay(), properties.getRetryMaxDelay(), false);
        this.self = self == null ? this : self;
    }

    public void process(KnowledgeIngestionJob claimedJob) {
        UUID jobId = claimedJob.getIngestionJobId();
        String workerId = claimedJob.getLockedBy();
        if (jobId == null || workerId == null || workerId.isBlank()) {
            return;
        }
        KnowledgeDocument document = documentPortOut.findById(claimedJob.getDocumentId()).orElse(null);
        if (document == null) {
            self.failDocumentMissing(jobId);
            return;
        }
        if (!self.prepareDocument(jobId, workerId, document.getDocumentId())) {
            return;
        }
        try {
            IngestionStage checkpoint = claimedJob.getLastCompletedStage();
            if (hasCompleted(checkpoint, IngestionStage.EMBEDDING)) {
                self.persistReview(jobId, workerId, document.getDocumentId());
                return;
            }

            List<KnowledgeDocumentBlock> blocks;
            if (!hasCompleted(checkpoint, IngestionStage.EXTRACTING)) {
                KnowledgeExtractionResult extraction = extract(document);
                if (extraction == null || extraction.isEmpty()) {
                    throw new KnowledgeWorkerException(KnowledgeDocumentFilePolicy.NO_EXTRACTABLE_TEXT,
                            "Không tìm thấy nội dung văn bản có thể trích xuất", false);
                }
                blocks = chunker.createBlocks(document.getDocumentId(), document.getDocumentVersion(), extraction);
                if (blocks.isEmpty()) {
                    throw new KnowledgeWorkerException(KnowledgeDocumentFilePolicy.NO_EXTRACTABLE_TEXT,
                            "Không tìm thấy nội dung văn bản có thể trích xuất", false);
                }
                if (!self.persistExtractionStage(jobId, workerId, document.getDocumentId(), blocks)) {
                    return;
                }
                checkpoint = IngestionStage.EXTRACTING;
            } else {
                blocks = blockPortOut.findByDocumentIdAndDocVersion(
                        document.getDocumentId(), document.getDocumentVersion());
                if (blocks.isEmpty()) {
                    throw new KnowledgeWorkerException("EXTRACTION_CHECKPOINT_INVALID",
                            "Checkpoint trích xuất không có dữ liệu khối nội dung", false);
                }
            }

            if (!hasCompleted(checkpoint, IngestionStage.CHUNKING)) {
                List<KnowledgeChunkDraft> chunks = chunker.createChunks(
                        document.getDocumentId(), document.getDocumentVersion(), document.getTitle(), blocks);
                KnowledgeChunkingResult chunking = new KnowledgeChunkingResult(blocks, chunks);
                if (chunking.isEmpty()) {
                    throw new KnowledgeWorkerException(KnowledgeDocumentFilePolicy.CHUNKING_FAILED,
                            "Tài liệu không tạo được chunk nội dung", false);
                }
                if (!self.persistChunksStage(jobId, workerId, document.getDocumentId(), chunking)) {
                    return;
                }
            }

            List<KnowledgeChunkSnapshot> persisted = chunkDraftPortOut.findByDocumentIdAndDocVersion(
                    document.getDocumentId(), document.getDocumentVersion());
            if (persisted.isEmpty()) {
                throw new KnowledgeWorkerException("CHUNK_PERSISTENCE_MISMATCH",
                        "Không tìm thấy chunk đã lưu cho tài liệu", true);
            }
            AiModelConfiguration configuration = findActiveEmbeddingConfiguration();
            List<KnowledgeStagedEmbedding> staged = embedAll(jobId, workerId, persisted, configuration);
            if (!self.persistStagedEmbeddings(jobId, workerId, staged)) {
                return;
            }

            self.persistReview(jobId, workerId, document.getDocumentId());
        } catch (KnowledgeWorkerException exception) {
            self.failOrRetry(jobId, workerId, document.getDocumentId(),
                    exception.code, exception.getMessageSafe(), exception.transientFailure);
        } catch (Exception exception) {
            log.warn("Unexpected knowledge ingestion failure for job {}: {}",
                    jobId, exception.getMessage(), exception);
            self.failOrRetry(jobId, workerId, document.getDocumentId(),
                    "UNKNOWN_INGESTION_ERROR", "Lỗi không xác định khi xử lý tài liệu", true);
        }
    }

    private KnowledgeExtractionResult extract(KnowledgeDocument document) {
        byte[] content = readContent(document);
        return extractionPort.extract(content, document.getFileExtension(), document.getMimeType());
    }

    private AiModelConfiguration findActiveEmbeddingConfiguration() {
        return configurationPortOut.findByUseCaseAndStatus(AiUseCase.EMBEDDING, AiModelStatus.ACTIVE)
                .stream()
                .findFirst()
                .orElseThrow(() -> new KnowledgeWorkerException("EMBEDDING_CONFIG_INVALID",
                        "Chưa có cấu hình model embedding đang hoạt động", false));
    }

    private List<KnowledgeStagedEmbedding> embedAll(
            UUID jobId,
            String workerId,
            List<KnowledgeChunkSnapshot> persisted,
            AiModelConfiguration configuration) {
        List<KnowledgeStagedEmbedding> staged = new ArrayList<>();
        for (KnowledgeChunkSnapshot chunk : persisted) {
            if (!renewHeartbeat(jobId, workerId)) {
                throw new KnowledgeWorkerException("INGESTION_LEASE_LOST",
                        "Mất quyền xử lý job, worker khác đã tiếp quản", false);
            }
            EmbeddingResult result = embedChunk(chunk, configuration);
            if (!result.isSuccess() || result.getVector() == null) {
                String code = result.getFailureCode() == null ? "EMBEDDING_FAILED" : result.getFailureCode();
                throw new KnowledgeWorkerException(code, "Không tính được vector nhúng cho chunk",
                        isTransient(code));
            }
            staged.add(KnowledgeStagedEmbedding.of(
                    chunk.getChunkId(),
                    configuration.getProvider(),
                    configuration.getConfigurationId(),
                    configuration.getModelId(),
                    configuration.getOutputDimension(),
                    promptFormatter.version(),
                    KnowledgeIndexVersionUseCaseImpl.CHUNKER_VERSION,
                    KnowledgeStagedEmbedding.sha256Hex(chunk.getContent() == null ? "" : chunk.getContent()),
                    result.getVector(),
                    Instant.now()));
        }
        return staged;
    }

    private EmbeddingResult embedChunk(KnowledgeChunkSnapshot chunk, AiModelConfiguration configuration) {
        String title = piiRedactionService.redact(chunk.getTitle() == null ? "" : chunk.getTitle()).value();
        String content = piiRedactionService.redact(chunk.getContent() == null ? "" : chunk.getContent()).value();
        String documentText = promptFormatter.formatDocument(promptFormatter.version(), title, content);
        return embeddingService.embed(new EmbeddingRequest(documentText, null), configuration);
    }

    private boolean renewHeartbeat(UUID jobId, String workerId) {
        if (jobPortOut.renewLease(jobId, workerId, leaseUntil())) {
            return true;
        }
        log.info("Knowledge ingestion lease lost for job {} (worker {}), skipping", jobId, workerId);
        return false;
    }

    private Instant leaseUntil() {
        return Instant.now().plus(properties.getLockDuration());
    }

    private byte[] readContent(KnowledgeDocument document) {
        try (InputStream stream = storagePort.openRead(document.getObjectKey())) {
            int limit = Math.toIntExact(KnowledgeDocumentFilePolicy.DEFAULT_MAX_FILE_SIZE_BYTES);
            byte[] content = stream.readNBytes(limit + 1);
            if (content.length > limit) {
                throw new KnowledgeWorkerException(
                        KnowledgeDocumentFilePolicy.FILE_TOO_LARGE,
                        "Tệp trong kho lưu trữ vượt quá dung lượng cho phép",
                        false);
            }
            return content;
        } catch (KnowledgeWorkerException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new KnowledgeWorkerException("STORAGE_READ_FAILED", "Không đọc được tệp trong kho lưu trữ", true);
        }
    }

    /** Claims worker bookkeeping in a short transaction; false when the lease is gone. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean prepareDocument(UUID jobId, String workerId, UUID documentId) {
        Instant now = Instant.now();
        KnowledgeIngestionJob job = jobPortOut.findById(jobId).orElse(null);
        if (job == null || !jobPortOut.renewLease(jobId, workerId, leaseUntil())) {
            return false;
        }
        KnowledgeDocument document = documentPortOut.findById(documentId).orElse(null);
        if (document == null) {
            return false;
        }
        document.markProcessing(now);
        documentPortOut.save(document);
        IngestionStage checkpoint = job.getLastCompletedStage();
        IngestionStage nextStage = checkpoint == null ? IngestionStage.EXTRACTING : checkpoint.next();
        job.advanceStage(nextStage, checkpoint == null ? 5 : progressFor(nextStage), now);
        jobPortOut.save(job);
        append(job, "JOB_PROCESSING", IngestionStage.EXTRACTING, "Bắt đầu xử lý tài liệu", now);
        return true;
    }

    /** Persists the extraction milestone in a short transaction. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean persistExtractionStage(
            UUID jobId,
            String workerId,
            UUID documentId,
            List<KnowledgeDocumentBlock> blocks) {
        Instant now = Instant.now();
        KnowledgeIngestionJob job = jobPortOut.findById(jobId).orElse(null);
        if (job == null || !jobPortOut.renewLease(jobId, workerId, leaseUntil())) {
            return false;
        }
        KnowledgeDocument document = documentPortOut.findById(documentId).orElse(null);
        if (document == null) {
            return false;
        }
        blockPortOut.replaceForVersion(documentId, document.getDocumentVersion(), blocks);
        job.completeStage(IngestionStage.EXTRACTING, IngestionStage.CHUNKING, 45, now);
        jobPortOut.save(job);
        append(job, "STAGE_EXTRACTING", IngestionStage.EXTRACTING, "Đọc và trích xuất nội dung", now);
        return true;
    }

    /** Replaces blocks and DRAFT chunks for this document version in a short transaction. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean persistChunksStage(
            UUID jobId, String workerId, UUID documentId, KnowledgeChunkingResult chunking) {
        Instant now = Instant.now();
        KnowledgeIngestionJob job = jobPortOut.findById(jobId).orElse(null);
        if (job == null || !jobPortOut.renewLease(jobId, workerId, leaseUntil())) {
            return false;
        }
        KnowledgeDocument document = documentPortOut.findById(documentId).orElse(null);
        if (document == null) {
            return false;
        }
        chunkDraftPortOut.replaceChunksForDocument(
                documentId,
                document.getDocumentVersion(),
                KnowledgeIndexVersionUseCaseImpl.CHUNKER_VERSION,
                chunking.chunks());
        job.completeStage(IngestionStage.CHUNKING, IngestionStage.EMBEDDING, 70, now);
        jobPortOut.save(job);
        append(job, "STAGE_CHUNKING", IngestionStage.CHUNKING, "Tách khối nội dung và tạo chunks", now);
        return true;
    }

    /** Persists all staged embeddings produced outside the transaction in one short commit. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean persistStagedEmbeddings(UUID jobId, String workerId, List<KnowledgeStagedEmbedding> staged) {
        Instant now = Instant.now();
        KnowledgeIngestionJob job = jobPortOut.findById(jobId).orElse(null);
        if (job == null || !jobPortOut.renewLease(jobId, workerId, leaseUntil())) {
            return false;
        }
        for (KnowledgeStagedEmbedding embedding : staged) {
            stagedEmbeddingPortOut.replaceForChunk(embedding.getChunkId(), List.of(embedding));
        }
        job.completeStage(IngestionStage.EMBEDDING, IngestionStage.REVIEW, 100, now);
        jobPortOut.save(job);
        append(job, "STAGE_EMBEDDING", IngestionStage.EMBEDDING, "Tính sẵn vector nhúng cho duyệt", now);
        return true;
    }

    /** Marks the document and job as ready for review in a short transaction. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistReview(UUID jobId, String workerId, UUID documentId) {
        Instant now = Instant.now();
        KnowledgeIngestionJob job = jobPortOut.findById(jobId).orElse(null);
        if (job == null || !jobPortOut.renewLease(jobId, workerId, leaseUntil())) {
            return;
        }
        KnowledgeDocument document = documentPortOut.findById(documentId).orElse(null);
        if (document != null) {
            document.markReview(now);
            documentPortOut.save(document);
        }
        job.enterReview(now);
        jobPortOut.save(job);
        append(job, "JOB_REVIEW", null, "Tài liệu đã sẵn sàng cho duyệt", now);
    }

    /** Handles a failing stage: classifies transient failures and schedules a backoff retry. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failOrRetry(UUID jobId, String workerId, UUID documentId, String code, String message, boolean retryable) {
        Instant now = Instant.now();
        KnowledgeIngestionJob job = jobPortOut.findByIdForUpdate(jobId).orElse(null);
        if (job == null) {
            return;
        }
        if (workerId != null && !workerId.equals(job.getLockedBy())) {
            return;
        }
        KnowledgeDocument document = documentPortOut.findById(documentId).orElse(null);
        if (document != null) {
            document.fail(code, now);
            documentPortOut.save(document);
        }
        if (retryable && job.isRetryable()) {
            long delayMillis = retryPolicy.nextDelayMillis(job.getAttemptCount(), null);
            job.scheduleRetry(now.plusMillis(delayMillis));
            jobPortOut.save(job);
            append(job, "JOB_RETRY_SCHEDULED", null,
                    "Lên lịch thử lại tiến trình sau " + Math.max(1, delayMillis / 1000) + " giây", now);
            return;
        }
        job.fail(code, message, now);
        jobPortOut.save(job);
        append(job, "JOB_FAILED", null, "Tiến trình xử lý thất bại: " + code, now);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failDocumentMissing(UUID jobId) {
        KnowledgeIngestionJob job = jobPortOut.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }
        Instant now = Instant.now();
        job.fail("DOCUMENT_NOT_FOUND", "Không tìm thấy tài liệu", now);
        jobPortOut.save(job);
        append(job, "JOB_FAILED", null, "Không tìm thấy tài liệu", now);
    }

    /** Codes that indicate a provider/temporary condition worth an automatic retry. */
    private static boolean isTransient(String code) {
        return switch (code == null ? "" : code) {
            case "EMBEDDING_FAILED", "EMBEDDING_RATE_LIMITED",
                 "EMBEDDING_TIMEOUT", "EMBEDDING_OVERLOADED", "STORAGE_READ_FAILED",
                 "CHUNK_PERSISTENCE_MISMATCH", "UNKNOWN_INGESTION_ERROR" -> true;
            default -> false;
        };
    }

    private static boolean hasCompleted(IngestionStage checkpoint, IngestionStage stage) {
        return checkpoint != null && checkpoint.ordinal() >= stage.ordinal();
    }

    private static int progressFor(IngestionStage stage) {
        return switch (stage) {
            case EXTRACTING -> 5;
            case CHUNKING -> 45;
            case EMBEDDING -> 70;
            case REVIEW -> 100;
        };
    }

    private void append(KnowledgeIngestionJob job, String type, IngestionStage stage, String detail, Instant now) {
        jobPortOut.appendEvent(KnowledgeIngestionJobEvent.of(job.getIngestionJobId(), type, stage, detail, now));
    }

    private static class KnowledgeWorkerException extends RuntimeException {
        private final String code;
        private final boolean transientFailure;

        private KnowledgeWorkerException(String code, String message, boolean transientFailure) {
            super(message);
            this.code = code;
            this.transientFailure = transientFailure;
        }

        String getMessageSafe() {
            String raw = getMessage();
            return raw == null || raw.length() <= 500 ? raw : raw.substring(0, 500);
        }
    }
}
