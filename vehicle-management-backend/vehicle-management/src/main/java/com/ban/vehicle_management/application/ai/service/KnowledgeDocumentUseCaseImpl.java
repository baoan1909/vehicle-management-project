package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.command.ArchiveKnowledgeDocumentCommand;
import com.ban.vehicle_management.application.ai.command.KnowledgeDocumentUploadResult;
import com.ban.vehicle_management.application.ai.command.PublishKnowledgeDocumentCommand;
import com.ban.vehicle_management.application.ai.command.ReindexKnowledgeDocumentCommand;
import com.ban.vehicle_management.application.ai.command.RejectKnowledgeDocumentCommand;
import com.ban.vehicle_management.application.ai.command.UploadKnowledgeDocumentCommand;
import com.ban.vehicle_management.application.ai.port.in.KnowledgeDocumentPortIn;
import com.ban.vehicle_management.application.ai.query.KnowledgeDocumentQuery;
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
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeChunkSnapshot;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeDocument;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeDocumentBlock;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeIngestionJob;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeIngestionJobEvent;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeSource;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeStagedEmbedding;
import com.ban.vehicle_management.domain.ai.knowledge.policy.KnowledgeDocumentFilePolicy;
import com.ban.vehicle_management.shared.enumeration.ai.IngestionStage;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeAccessScope;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeChunkStatus;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeDocumentStatus;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeSourceStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.KnowledgeFileValidationException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class KnowledgeDocumentUseCaseImpl implements KnowledgeDocumentPortIn {

    private final CurrentAccountPortIn currentAccountPortIn;
    private final KnowledgeSourcePortOut sourcePortOut;
    private final KnowledgeDocumentPortOut documentPortOut;
    private final KnowledgeIngestionJobPortOut ingestionJobPortOut;
    private final KnowledgeDocumentBlockPortOut blockPortOut;
    private final KnowledgeChunkDraftPortOut chunkDraftPortOut;
    private final KnowledgeStagedEmbeddingPortOut stagedEmbeddingPortOut;
    private final KnowledgeDocumentStoragePort storagePort;
    private final KnowledgeDocumentValidationPortOut validationPort;
    private final KnowledgeIngestionProperties properties;
    private final KnowledgeIndexBuildCoordinator indexBuildCoordinator;
    private final AiModelConfigurationPortOut configurationPortOut;
    private final EmbeddingPromptFormatter promptFormatter;

    public KnowledgeDocumentUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            KnowledgeSourcePortOut sourcePortOut,
            KnowledgeDocumentPortOut documentPortOut,
            KnowledgeIngestionJobPortOut ingestionJobPortOut,
            KnowledgeDocumentBlockPortOut blockPortOut,
            KnowledgeChunkDraftPortOut chunkDraftPortOut,
            KnowledgeStagedEmbeddingPortOut stagedEmbeddingPortOut,
            KnowledgeDocumentStoragePort storagePort,
            KnowledgeDocumentValidationPortOut validationPort,
            KnowledgeIngestionProperties properties,
            KnowledgeIndexBuildCoordinator indexBuildCoordinator,
            AiModelConfigurationPortOut configurationPortOut,
            EmbeddingPromptFormatter promptFormatter) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.sourcePortOut = sourcePortOut;
        this.documentPortOut = documentPortOut;
        this.ingestionJobPortOut = ingestionJobPortOut;
        this.blockPortOut = blockPortOut;
        this.chunkDraftPortOut = chunkDraftPortOut;
        this.stagedEmbeddingPortOut = stagedEmbeddingPortOut;
        this.storagePort = storagePort;
        this.validationPort = validationPort;
        this.properties = properties;
        this.indexBuildCoordinator = indexBuildCoordinator;
        this.configurationPortOut = configurationPortOut;
        this.promptFormatter = promptFormatter;
    }

    @Override
    @Transactional
    public KnowledgeDocumentUploadResult uploadDocument(UploadKnowledgeDocumentCommand command) {
        currentAccountPortIn.requirePermission("AI_KNOWLEDGE_MANAGE_ALL");
        UUID actor = currentAccountPortIn.getCurrentAccountIdOrThrow();
        String idempotencyKey = TextValidationUtils.normalizeRequiredText(
                command.idempotencyKey(), "idempotencyKey", 120);
        KnowledgeSource source = sourcePortOut.findById(command.sourceId())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nguồn kiến thức"));
        if (KnowledgeSourceStatus.INACTIVE.equals(source.getStatus())) {
            throw new BadRequestException("Nguồn kiến thức đang ở trạng thái ngừng hoạt động");
        }

        String originalFilename = TextValidationUtils.normalizeRequiredText(
                command.originalFilename(), "originalFilename", 255);
        String extension = KnowledgeDocumentFilePolicy.resolveExtension(originalFilename);
        if (!KnowledgeDocumentFilePolicy.isSupported(extension)) {
            throw new KnowledgeFileValidationException(
                    KnowledgeDocumentFilePolicy.UNSUPPORTED_FILE_TYPE,
                    "Định dạng tệp ." + extension + " chưa được hỗ trợ");
        }
        if (command.content() == null || command.content().length == 0) {
            throw new KnowledgeFileValidationException(
                    KnowledgeDocumentFilePolicy.EMPTY_FILE, "Tệp tải lên rỗng");
        }
        validationPort.validate(
                command.content(),
                extension,
                KnowledgeDocumentFilePolicy.DEFAULT_MAX_FILE_SIZE_BYTES);

        String checksum = sha256Hex(command.content());
        KnowledgeIngestionJob existingJob = ingestionJobPortOut
                .findByRequestedByAndIdempotencyKey(actor, idempotencyKey)
                .orElse(null);
        if (existingJob != null) {
            KnowledgeDocument existingDocument = documentPortOut.findById(existingJob.getDocumentId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Idempotent ingestion job points to a missing document"));
            if (!source.getSourceId().equals(existingDocument.getSourceId())
                    || !checksum.equals(existingDocument.getChecksumSha256())) {
                throw new ConflictException("Idempotency-Key đã được dùng cho một tài liệu khác");
            }
            return KnowledgeDocumentUploadResult.duplicate(existingDocument);
        }
        KnowledgeDocument existing = documentPortOut.findBySourceIdAndChecksum(source.getSourceId(), checksum)
                .orElse(null);
        if (existing != null) {
            return KnowledgeDocumentUploadResult.duplicate(existing);
        }

        Instant now = Instant.now();
        String title = TextValidationUtils.normalizeRequiredText(
                command.title() == null || command.title().isBlank()
                        ? defaultTitle(originalFilename)
                        : command.title(),
                "title",
                240);
        String mimeType = KnowledgeDocumentFilePolicy.canonicalMimeType(extension)
                .orElse("application/octet-stream");
        String objectKey = storagePort.store(
                command.content(), mimeType, UUID.randomUUID(), extension);
        KnowledgeDocument draft = KnowledgeDocument.newVersion(
                source.getSourceId(),
                UUID.randomUUID(),
                null,
                source.getTenantId(),
                title,
                objectKey,
                originalFilename,
                extension,
                mimeType,
                (long) command.content().length,
                checksum,
                1,
                source.getAccessScope(),
                now);
        KnowledgeDocument persisted = documentPortOut.insertIdempotent(draft);
        if (!persisted.getDocumentId().equals(draft.getDocumentId())) {
            storagePort.delete(objectKey);
            return KnowledgeDocumentUploadResult.duplicate(persisted);
        }

        KnowledgeIngestionJob job = KnowledgeIngestionJob.create(
                persisted.getDocumentId(),
                properties.getMaxAttempts(),
                idempotencyKey,
                actor,
                now);
        ingestionJobPortOut.save(job);
        ingestionJobPortOut.appendEvent(KnowledgeIngestionJobEvent.of(
                job.getIngestionJobId(), "JOB_CREATED", IngestionStage.EXTRACTING, "Tài liệu đã được tải lên", now));
        return KnowledgeDocumentUploadResult.created(persisted);
    }

    @Override
    @Transactional
    public KnowledgeDocument publishDocument(PublishKnowledgeDocumentCommand command) {
        currentAccountPortIn.requirePermission("AI_KNOWLEDGE_APPROVE_ALL");
        UUID actor = currentAccountPortIn.getCurrentAccountIdOrThrow();
        Instant now = Instant.now();
        KnowledgeDocument document = lockDocument(command.documentId());
        if (document.getStatus() != KnowledgeDocumentStatus.REVIEW) {
            throw new BadRequestException("Tài liệu chỉ có thể duyệt khi đang chờ phê duyệt");
        }
        assertEmbeddingCoverage(document);
        document.approveForPublish(actor, now);
        KnowledgeDocument saved = documentPortOut.save(document);
        chunkDraftPortOut.updateStatusByDocumentId(
                saved.getDocumentId(), KnowledgeChunkStatus.DRAFT, KnowledgeChunkStatus.READY);
        // Do NOT retire superseded version chunks - they remain READY in the ACTIVE index.
        // The next index build will only include the latest version per document_key.
        KnowledgeIngestionJob openJob = completeOpenJob(saved, now);
        if (openJob == null) {
            appendJobEvent(saved, "DOCUMENT_APPROVED", "Tài liệu đã được duyệt", now);
        }
        indexBuildCoordinator.ensureAutoCandidate();
        return saved;
    }

    @Override
    @Transactional
    public KnowledgeDocument rejectDocument(RejectKnowledgeDocumentCommand command) {
        currentAccountPortIn.requirePermission("AI_KNOWLEDGE_APPROVE_ALL");
        UUID actor = currentAccountPortIn.getCurrentAccountIdOrThrow();
        Instant now = Instant.now();
        KnowledgeDocument document = lockDocument(command.documentId());
        if (document.getStatus() != KnowledgeDocumentStatus.REVIEW) {
            throw new BadRequestException("Tài liệu chỉ có thể từ chối khi đang chờ phê duyệt");
        }
        deleteChunksAndStagedEmbeds(document);
        document.fail("DOCUMENT_REJECTED", now);
        documentPortOut.save(document);
        ingestionJobPortOut.findLatestByDocumentId(document.getDocumentId())
                .ifPresent(job -> {
                    job.fail("DOCUMENT_REJECTED", "Tài liệu bị từ chối khi chờ duyệt", now);
                    ingestionJobPortOut.save(job);
                    ingestionJobPortOut.appendEvent(KnowledgeIngestionJobEvent.of(
                            job.getIngestionJobId(), "DOCUMENT_REJECTED",
                            IngestionStage.REVIEW, "Tài liệu bị từ chối, vui lòng tải lại bản đúng chuẩn", now));
                });
        return document;
    }

    @Override
    @Transactional
    public KnowledgeDocument archiveDocument(ArchiveKnowledgeDocumentCommand command) {
        currentAccountPortIn.requirePermission("AI_KNOWLEDGE_APPROVE_ALL");
        UUID actor = currentAccountPortIn.getCurrentAccountIdOrThrow();
        Instant now = Instant.now();
        KnowledgeDocument document = lockDocument(command.documentId());
        if (document.getStatus() == KnowledgeDocumentStatus.ARCHIVED) {
            return document;
        }
        if (document.getStatus() != KnowledgeDocumentStatus.READY
                && document.getStatus() != KnowledgeDocumentStatus.REVIEW) {
            throw new BadRequestException("Chỉ tài liệu đã duyệt hoặc đang chờ duyệt mới có thể lưu trữ");
        }
        document.archive(actor, now);
        KnowledgeDocument saved = documentPortOut.save(document);
        chunkDraftPortOut.updateStatusByDocumentId(
                saved.getDocumentId(), KnowledgeChunkStatus.READY, KnowledgeChunkStatus.ARCHIVED);
        appendJobEvent(saved, "DOCUMENT_ARCHIVED", "Tài liệu đã được lưu trữ", now);
        indexBuildCoordinator.onCorpusChanged();
        return saved;
    }

    @Override
    @Transactional
    public KnowledgeDocument reindexDocument(ReindexKnowledgeDocumentCommand command) {
        currentAccountPortIn.requirePermission("AI_KNOWLEDGE_REINDEX_ALL");
        UUID actor = currentAccountPortIn.getCurrentAccountIdOrThrow();
        Instant now = Instant.now();
        String idempotencyKey = TextValidationUtils.normalizeRequiredText(
                command.idempotencyKey(), "idempotencyKey", 120);
        KnowledgeIngestionJob existingJob = ingestionJobPortOut
                .findByRequestedByAndIdempotencyKey(actor, idempotencyKey)
                .orElse(null);
        if (existingJob != null) {
            KnowledgeDocument requested = documentPortOut.findById(command.documentId())
                    .orElseThrow(() -> new NotFoundException("Không tìm thấy tài liệu"));
            KnowledgeDocument previousResult = documentPortOut.findById(existingJob.getDocumentId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Idempotent ingestion job points to a missing document"));
            if (!requested.getDocumentKey().equals(previousResult.getDocumentKey())
                    || previousResult.getDocumentVersion() <= requested.getDocumentVersion()) {
                throw new ConflictException("Idempotency-Key đã được dùng cho một yêu cầu khác");
            }
            return previousResult;
        }

        // First find the document by ID to get its document_key
        KnowledgeDocument document = lockDocument(command.documentId());
        if (document.getStatus() != KnowledgeDocumentStatus.READY
                && document.getStatus() != KnowledgeDocumentStatus.FAILED) {
            throw new BadRequestException("Chỉ tài liệu đã duyệt hoặc thất bại mới có thể lập chỉ mục lại");
        }

        // Lock the latest READY version by document_key to ensure correct version sequencing
        KnowledgeDocumentPortOut.LockedDocumentVersion lockedVersion = documentPortOut.lockLatestVersionByDocumentKey(document.getDocumentKey())
                .orElseThrow(() -> new NotFoundException("Không tìm thấy phiên bản tài liệu sẵn sàng để lập chỉ mục lại"));

        if (lockedVersion.status() == KnowledgeDocumentStatus.PENDING
                || lockedVersion.status() == KnowledgeDocumentStatus.PROCESSING
                || lockedVersion.status() == KnowledgeDocumentStatus.REVIEW) {
            throw new BadRequestException("Tài liệu đang có một phiên bản được xử lý, không thể lập chỉ mục lại đồng thời");
        }

        int nextVersion = lockedVersion.maxVersion() + 1;

        KnowledgeDocument newVersion = KnowledgeDocument.newVersion(
                document.getSourceId(),
                document.getDocumentKey(),
                lockedVersion.documentId(),
                document.getTenantId(),
                document.getTitle(),
                document.getObjectKey(),
                document.getOriginalFilename(),
                document.getFileExtension(),
                document.getMimeType(),
                document.getFileSizeBytes(),
                document.getChecksumSha256(),
                nextVersion,
                document.getAccessScope(),
                now);
        KnowledgeDocument saved = documentPortOut.insertIdempotent(newVersion);
        KnowledgeIngestionJob job = KnowledgeIngestionJob.create(
                saved.getDocumentId(), properties.getMaxAttempts(), idempotencyKey, actor, now);
        ingestionJobPortOut.save(job);
        ingestionJobPortOut.appendEvent(KnowledgeIngestionJobEvent.of(
                job.getIngestionJobId(), "JOB_CREATED", IngestionStage.EXTRACTING, "Lập chỉ mục lại tài liệu", now));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeDocument documentDetail(UUID documentId) {
        currentAccountPortIn.requirePermission("AI_KNOWLEDGE_READ_ALL");
        return documentPortOut.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài liệu"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeDocument> listDocuments() {
        currentAccountPortIn.requirePermission("AI_KNOWLEDGE_READ_ALL");
        return documentPortOut.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<KnowledgeDocument> listDocuments(KnowledgeDocumentQuery query, Pageable pageable) {
        currentAccountPortIn.requirePermission("AI_KNOWLEDGE_READ_ALL");
        return documentPortOut.findAll(query, KnowledgePagePolicy.normalize(pageable));
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeDocumentBlock> documentBlocks(UUID documentId, int documentVersion) {
        currentAccountPortIn.requirePermission("AI_KNOWLEDGE_READ_ALL");
        documentDetail(documentId);
        return blockPortOut.findByDocumentIdAndDocVersion(documentId, documentVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeChunkSnapshot> documentChunks(UUID documentId, int documentVersion) {
        currentAccountPortIn.requirePermission("AI_KNOWLEDGE_READ_ALL");
        documentDetail(documentId);
        return chunkDraftPortOut.findByDocumentIdAndDocVersion(documentId, documentVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeDocument latestBySourceAndChecksum(UUID sourceId, String checksumSha256) {
        currentAccountPortIn.requirePermission("AI_KNOWLEDGE_READ_ALL");
        return documentPortOut.findBySourceIdAndChecksum(sourceId, checksumSha256)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài liệu"));
    }

    private KnowledgeDocument lockDocument(UUID documentId) {
        return documentPortOut.findByIdForUpdate(documentId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tài liệu"));
    }

    /**
     * Publish gate: every chunk of this document version must already carry a staged
     * embedding produced before review, so approving never publishes a chunk the index
     * cannot retrieve by vector.
     * Verifies all embedding properties match the active configuration.
     */
    private void assertEmbeddingCoverage(KnowledgeDocument document) {
        List<KnowledgeChunkSnapshot> chunks = chunkDraftPortOut.findByDocumentIdAndDocVersion(
                document.getDocumentId(), document.getDocumentVersion());
        List<UUID> chunkIds = chunks.stream()
                .map(KnowledgeChunkSnapshot::getChunkId)
                .toList();
        if (chunkIds.isEmpty()) {
            throw new BadRequestException("Tài liệu chưa có chunk nội dung, không thể duyệt");
        }

        // Get active embedding configuration for verification
        AiModelConfiguration activeConfig = configurationPortOut.findByUseCaseAndStatus(
                        com.ban.vehicle_management.shared.enumeration.ai.AiUseCase.EMBEDDING,
                        com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus.ACTIVE)
                .stream()
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Không có cấu hình embedding đang hoạt động"));

        // Verify staged embeddings exist and match configuration
        List<KnowledgeStagedEmbedding> stagedEmbeddings = stagedEmbeddingPortOut.findByChunkIds(chunkIds);
        if (stagedEmbeddings.size() != chunkIds.size()) {
            throw new BadRequestException(
                    "Chưa đủ vector nhúng cho toàn bộ chunk, không thể duyệt tài liệu");
        }

        for (KnowledgeStagedEmbedding staged : stagedEmbeddings) {
            UUID chunkId = staged.getChunkId();
            KnowledgeChunkSnapshot chunk = chunks.stream()
                    .filter(c -> c.getChunkId().equals(chunkId))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Chunk không tồn tại: " + chunkId));

            // Verify vector exists and is not null
            if (staged.getEmbedding() == null || staged.getEmbedding().values() == null) {
                throw new BadRequestException(
                        "Vector nhúng rỗng cho chunk " + chunkId + ", không thể duyệt tài liệu");
            }

            // Verify vector dimension
            if (staged.getEmbedding().dimension() != activeConfig.getOutputDimension()) {
                throw new BadRequestException(
                        "Sai dimension vector nhúng cho chunk " + chunkId
                                + " (mong đợi " + activeConfig.getOutputDimension()
                                + ", thực tế " + staged.getEmbedding().dimension() + ")");
            }

            // Verify provider
            if (!activeConfig.getProvider().equals(staged.getProvider())) {
                throw new BadRequestException(
                        "Sai provider embedding cho chunk " + chunkId);
            }

            // Verify model configuration ID
            if (!activeConfig.getConfigurationId().equals(staged.getModelConfigurationId())) {
                throw new BadRequestException(
                        "Sai cấu hình model embedding cho chunk " + chunkId);
            }

            // Verify model ID
            if (!activeConfig.getModelId().equals(staged.getModelId())) {
                throw new BadRequestException(
                        "Sai model embedding cho chunk " + chunkId);
            }

            // Verify chunker version
            if (!KnowledgeIndexVersionUseCaseImpl.CHUNKER_VERSION.equals(staged.getChunkerVersion())) {
                throw new BadRequestException(
                        "Sai phiên bản chunker cho chunk " + chunkId);
            }

            if (!promptFormatter.version().equals(staged.getEmbeddingPromptVersion())) {
                throw new BadRequestException(
                        "Sai phiên bản embedding prompt cho chunk " + chunkId);
            }

            // Verify content hash matches
            String expectedHash = KnowledgeStagedEmbedding.sha256Hex(chunk.getContent() == null ? "" : chunk.getContent());
            if (!expectedHash.equals(staged.getContentHash())) {
                throw new BadRequestException(
                        "Content hash không khớp cho chunk " + chunkId);
            }

            // Verify embedding fingerprint
            String expectedFingerprint = KnowledgeStagedEmbedding.fingerprint(
                    staged.getContentHash(),
                    staged.getModelId(),
                    promptFormatter.version(),
                    KnowledgeIndexVersionUseCaseImpl.CHUNKER_VERSION);
            if (!expectedFingerprint.equals(staged.getEmbeddingFingerprint())) {
                throw new BadRequestException(
                        "Embedding fingerprint không khớp cho chunk " + chunkId);
            }
        }
    }

    private KnowledgeIngestionJob completeOpenJob(KnowledgeDocument document, Instant now) {
        return ingestionJobPortOut.findOpenByDocumentId(document.getDocumentId())
                .stream()
                .findFirst()
                .map(job -> {
                    job.complete(now);
                    ingestionJobPortOut.save(job);
                    ingestionJobPortOut.appendEvent(KnowledgeIngestionJobEvent.of(
                            job.getIngestionJobId(), "JOB_COMPLETED", null, "Xử lý hoàn tất", now));
                    return job;
                })
                .orElse(null);
    }

    private void appendJobEvent(KnowledgeDocument document, String eventType, String detail, Instant now) {
        ingestionJobPortOut.findLatestByDocumentId(document.getDocumentId())
                .ifPresent(job -> ingestionJobPortOut.appendEvent(KnowledgeIngestionJobEvent.of(
                        job.getIngestionJobId(), eventType, null, detail, now)));
    }

    private void deleteChunksAndStagedEmbeds(KnowledgeDocument document) {
        List<UUID> chunkIds = chunkDraftPortOut.findByDocumentIdAndDocVersion(
                        document.getDocumentId(), document.getDocumentVersion())
                .stream()
                .map(KnowledgeChunkSnapshot::getChunkId)
                .toList();
        if (!chunkIds.isEmpty()) {
            stagedEmbeddingPortOut.deleteByChunkIds(chunkIds);
        }
        chunkDraftPortOut.replaceChunksForDocument(
                document.getDocumentId(), document.getDocumentVersion(), "CLEANUP", List.of());
    }

    /**
     * When an immutable reindex version is approved, the version it supersedes leaves the
     * ACTIVE corpus: its READY chunks are archived. The old document row is kept READY as
     * a frozen historical version while its chunks no longer participate in retrieval.
     */
    private void retireSupersededVersionChunks(KnowledgeDocument publishedVersion) {
        if (publishedVersion.getSupersedesDocumentId() == null) {
            return;
        }
        chunkDraftPortOut.updateStatusByDocumentId(
                publishedVersion.getSupersedesDocumentId(),
                KnowledgeChunkStatus.READY,
                KnowledgeChunkStatus.ARCHIVED);
    }

    private String defaultTitle(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "Tài liệu";
        }
        int dot = originalFilename.lastIndexOf('.');
        String base = dot > 0 ? originalFilename.substring(0, dot) : originalFilename;
        return base.isBlank() ? "Tài liệu" : base;
    }

    private String sha256Hex(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(content));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

}
