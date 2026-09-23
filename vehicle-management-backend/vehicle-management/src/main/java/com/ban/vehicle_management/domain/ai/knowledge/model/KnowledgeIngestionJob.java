package com.ban.vehicle_management.domain.ai.knowledge.model;

import com.ban.vehicle_management.shared.enumeration.ai.IngestionStage;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIngestionJobStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Executes the extraction -> chunking -> embedding pipeline for exactly one document.
 * Workers claim it with a short transaction and renew a lease by heartbeat; progress
 * is tracked alongside the status (status = lifecycle, stage = sub-progress).
 */
@Getter
@Setter
public class KnowledgeIngestionJob {

    private UUID ingestionJobId;
    private UUID documentId;
    private KnowledgeIngestionJobStatus status;
    private IngestionStage currentStage;
    private Integer progressPercent;
    private int attemptCount;
    private int maxAttempts;
    private IngestionStage lastCompletedStage;
    private String lockedBy;
    private Instant lockedAt;
    private Instant lockExpiresAt;
    private Instant lastHeartbeatAt;
    private Instant startedAt;
    private Instant completedAt;
    private String errorCode;
    private String errorMessageRedacted;
    private String idempotencyKey;
    private UUID requestedBy;
    private String embeddingFingerprint;
    private Instant nextAttemptAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static KnowledgeIngestionJob create(
            UUID documentId,
            int maxAttempts,
            String idempotencyKey,
            UUID requestedBy,
            Instant now) {
        KnowledgeIngestionJob job = new KnowledgeIngestionJob();
        job.ingestionJobId = UUID.randomUUID();
        job.documentId = documentId;
        job.status = KnowledgeIngestionJobStatus.PENDING;
        job.attemptCount = 0;
        job.maxAttempts = maxAttempts;
        job.idempotencyKey = idempotencyKey;
        job.requestedBy = requestedBy;
        job.nextAttemptAt = now;
        job.createdAt = now;
        job.updatedAt = now;
        return job;
    }

    public boolean isOpen() {
        return switch (status) {
            case PENDING, PROCESSING, RETRYING, REVIEW -> true;
            case READY, FAILED -> false;
        };
    }

    public boolean isRetryable() {
        return attemptCount < maxAttempts;
    }

    /** Claimed by a worker: status PROCESSING, stage EXTRACTING, lease acquired. */
    public void claim(String workerId, int attempt, Instant now, Instant leaseUntil) {
        this.status = KnowledgeIngestionJobStatus.PROCESSING;
        this.currentStage = IngestionStage.EXTRACTING;
        this.progressPercent = 0;
        this.attemptCount = attempt;
        this.lockedBy = workerId;
        this.lockedAt = now;
        this.lockExpiresAt = leaseUntil;
        this.lastHeartbeatAt = now;
        this.startedAt = now;
        this.errorCode = null;
        this.errorMessageRedacted = null;
        this.updatedAt = now;
    }

    public void advanceStage(IngestionStage stage, int percent, Instant now) {
        this.currentStage = stage;
        this.progressPercent = percent;
        this.lastHeartbeatAt = now;
        this.updatedAt = now;
    }

    public void completeStage(
            IngestionStage completedStage,
            IngestionStage nextStage,
            int percent,
            Instant now) {
        this.lastCompletedStage = completedStage;
        advanceStage(nextStage, percent, now);
    }

    /** Worker renewed its lease; fails silently when the lock is held by someone else. */
    public boolean renewLease(String workerId, Instant leaseUntil, Instant now) {
        if (!lockedBy.equals(workerId) || status != KnowledgeIngestionJobStatus.PROCESSING) {
            return false;
        }
        this.lockExpiresAt = leaseUntil;
        this.lastHeartbeatAt = now;
        this.updatedAt = now;
        return true;
    }

    public void enterReview(Instant now) {
        this.status = KnowledgeIngestionJobStatus.REVIEW;
        this.currentStage = IngestionStage.REVIEW;
        this.progressPercent = 100;
        this.lastCompletedStage = IngestionStage.EMBEDDING;
        this.completedAt = now;
        this.updatedAt = now;
    }

    public void complete(Instant now) {
        this.status = KnowledgeIngestionJobStatus.READY;
        this.currentStage = null;
        this.progressPercent = 100;
        this.completedAt = now;
        this.updatedAt = now;
    }

    public void fail(String errorCode, String messageRedacted, Instant now) {
        this.status = KnowledgeIngestionJobStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessageRedacted = messageRedacted;
        this.completedAt = now;
        this.updatedAt = now;
    }

    public void scheduleRetry(Instant nextAttempt) {
        this.status = KnowledgeIngestionJobStatus.RETRYING;
        this.currentStage = null;
        this.nextAttemptAt = nextAttempt;
        this.updatedAt = Instant.now();
    }

    public void resetForRetry(Instant now) {
        this.status = KnowledgeIngestionJobStatus.PENDING;
        this.currentStage = null;
        this.attemptCount = 0;
        this.errorCode = null;
        this.errorMessageRedacted = null;
        this.nextAttemptAt = now;
        this.lockedBy = null;
        this.lockExpiresAt = null;
        this.updatedAt = now;
    }
}
