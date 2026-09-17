package com.ban.vehicle_management.infrastructure.persistence.database.entity.ai;

import com.ban.vehicle_management.shared.enumeration.ai.IngestionStage;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIngestionJobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "knowledge_ingestion_jobs", schema = "ai")
@Getter
@Setter
public class KnowledgeIngestionJobEntity {

    @Id
    @Column(name = "ingestion_job_id", nullable = false)
    private UUID ingestionJobId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private KnowledgeIngestionJobStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_stage")
    private IngestionStage currentStage;

    @Column(name = "progress_percent")
    private Integer progressPercent;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_completed_stage")
    private IngestionStage lastCompletedStage;

    @Column(name = "locked_by")
    private String lockedBy;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "lock_expires_at")
    private Instant lockExpiresAt;

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message_redacted")
    private String errorMessageRedacted;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Column(name = "requested_by")
    private UUID requestedBy;

    @Column(name = "embedding_fingerprint")
    private String embeddingFingerprint;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}