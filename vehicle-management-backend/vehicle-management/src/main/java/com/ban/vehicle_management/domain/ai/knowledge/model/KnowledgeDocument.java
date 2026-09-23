package com.ban.vehicle_management.domain.ai.knowledge.model;

import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeAccessScope;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeDocumentStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * A physical uploaded file with a logical identity. documentKey groups the version
 * history of the same logical document; supersedesDocumentId points to the row this
 * version replaced.
 */
@Getter
@Setter
public class KnowledgeDocument {

    private UUID documentId;
    private UUID sourceId;
    private UUID documentKey;
    private UUID supersedesDocumentId;
    private UUID tenantId;
    private String title;
    private String objectKey;
    private String originalFilename;
    private String fileExtension;
    private String mimeType;
    private Long fileSizeBytes;
    private String checksumSha256;
    private Integer documentVersion;
    private KnowledgeAccessScope accessScope;
    private KnowledgeDocumentStatus status;
    private String failureCode;
    private Instant effectiveFrom;
    private Instant effectiveTo;
    private Instant reviewedAt;
    private UUID reviewedBy;
    private Instant archivedAt;
    private UUID archivedBy;
    private Instant createdAt;
    private Instant updatedAt;

    public static KnowledgeDocument newVersion(
            UUID sourceId,
            UUID documentKey,
            UUID supersedesDocumentId,
            UUID tenantId,
            String title,
            String objectKey,
            String originalFilename,
            String fileExtension,
            String mimeType,
            Long fileSizeBytes,
            String checksumSha256,
            int documentVersion,
            KnowledgeAccessScope accessScope,
            Instant now) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.documentId = UUID.randomUUID();
        document.sourceId = sourceId;
        document.documentKey = documentKey;
        document.supersedesDocumentId = supersedesDocumentId;
        document.tenantId = tenantId;
        document.title = title;
        document.objectKey = objectKey;
        document.originalFilename = originalFilename;
        document.fileExtension = fileExtension;
        document.mimeType = mimeType;
        document.fileSizeBytes = fileSizeBytes;
        document.checksumSha256 = checksumSha256;
        document.documentVersion = documentVersion;
        document.accessScope = accessScope;
        document.status = KnowledgeDocumentStatus.PENDING;
        document.effectiveFrom = now;
        document.createdAt = now;
        document.updatedAt = now;
        return document;
    }

    /** Worker started extraction. */
    public void markProcessing(Instant now) {
        this.status = KnowledgeDocumentStatus.PROCESSING;
        this.failureCode = null;
        this.updatedAt = now;
    }

    /** Extraction/chunking/embedding succeeded and the reviewer may approve it. */
    public void markReview(Instant now) {
        if (status != KnowledgeDocumentStatus.PROCESSING) {
            throw new IllegalStateException("Only PROCESSING documents can enter REVIEW");
        }
        this.status = KnowledgeDocumentStatus.REVIEW;
        this.updatedAt = now;
    }

    /** Reviewer approved the document; chunks are promoted together with this call. */
    public void approveForPublish(UUID reviewer, Instant now) {
        if (status != KnowledgeDocumentStatus.REVIEW
                && status != KnowledgeDocumentStatus.PENDING
                && status != KnowledgeDocumentStatus.PROCESSING) {
            throw new IllegalStateException("Document is not reviewable in state " + status);
        }
        this.status = KnowledgeDocumentStatus.READY;
        this.reviewedAt = now;
        this.reviewedBy = reviewer;
        this.failureCode = null;
        this.effectiveTo = null;
        this.updatedAt = now;
    }

    /** Hide the document immediately; its chunks are archived together with it. */
    public void archive(UUID actor, Instant now) {
        if (status == KnowledgeDocumentStatus.ARCHIVED) {
            return;
        }
        this.status = KnowledgeDocumentStatus.ARCHIVED;
        this.archivedAt = now;
        this.archivedBy = actor;
        this.effectiveTo = now;
        this.updatedAt = now;
    }

    public void fail(String failureCode, Instant now) {
        this.status = KnowledgeDocumentStatus.FAILED;
        this.failureCode = failureCode;
        this.updatedAt = now;
    }

    public void resetForNewAttempt(Instant now) {
        this.status = KnowledgeDocumentStatus.PENDING;
        this.failureCode = null;
        this.updatedAt = now;
    }
}