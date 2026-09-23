package com.ban.vehicle_management.infrastructure.persistence.database.repository.ai;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.KnowledgeIngestionJobEntity;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIngestionJobStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KnowledgeIngestionJobRepository extends JpaRepository<KnowledgeIngestionJobEntity, UUID>, JpaSpecificationExecutor<KnowledgeIngestionJobEntity> {

    Optional<KnowledgeIngestionJobEntity> findByDocumentId(UUID documentId);

    Optional<KnowledgeIngestionJobEntity> findFirstByDocumentIdOrderByCreatedAtDesc(UUID documentId);

    Optional<KnowledgeIngestionJobEntity> findByRequestedByAndIdempotencyKey(UUID requestedBy, String idempotencyKey);

    List<KnowledgeIngestionJobEntity> findByDocumentIdAndStatusInOrderByCreatedAtDesc(
            UUID documentId, List<KnowledgeIngestionJobStatus> statuses);

    List<KnowledgeIngestionJobEntity> findByStatusOrderByCreatedAtAsc(KnowledgeIngestionJobStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT job FROM KnowledgeIngestionJobEntity job WHERE job.ingestionJobId = :ingestionJobId")
    Optional<KnowledgeIngestionJobEntity> findByIdForUpdate(@Param("ingestionJobId") UUID ingestionJobId);

    @Query(value = """
            WITH due_jobs AS (
                SELECT ingestion_job_id
                FROM ai.knowledge_ingestion_jobs
                WHERE (
                    status IN ('PENDING', 'RETRYING')
                    AND next_attempt_at <= :now
                ) OR (
                    status = 'PROCESSING'
                    AND lock_expires_at IS NOT NULL
                    AND lock_expires_at <= :now
                )
                ORDER BY created_at ASC
                FOR UPDATE SKIP LOCKED
                LIMIT :limit
            )
            UPDATE ai.knowledge_ingestion_jobs job
            SET status = 'PROCESSING',
                current_stage = 'EXTRACTING',
                attempt_count = job.attempt_count + 1,
                locked_at = :now,
                locked_by = :lockedBy,
                lock_expires_at = :lockExpiresAt,
                last_heartbeat_at = :now,
                started_at = COALESCE(job.started_at, :now),
                error_code = NULL,
                error_message_redacted = NULL
            FROM due_jobs
            WHERE job.ingestion_job_id = due_jobs.ingestion_job_id
            RETURNING job.*
            """, nativeQuery = true)
    List<KnowledgeIngestionJobEntity> claimDueJobs(
            @Param("now") Instant now,
            @Param("lockExpiresAt") Instant lockExpiresAt,
            @Param("lockedBy") String lockedBy,
            @Param("limit") int limit
    );

    @Modifying
    @Query(value = """
            UPDATE ai.knowledge_ingestion_jobs
            SET lock_expires_at = :lockExpiresAt,
                last_heartbeat_at = :now,
                updated_at = :now
            WHERE ingestion_job_id = :ingestionJobId
              AND locked_by = :lockedBy
              AND status = 'PROCESSING'
            """, nativeQuery = true)
    int renewLease(
            @Param("ingestionJobId") UUID ingestionJobId,
            @Param("lockedBy") String lockedBy,
            @Param("lockExpiresAt") Instant lockExpiresAt,
            @Param("now") Instant now
    );
}
