package com.ban.vehicle_management.infrastructure.persistence.database.repository.ai;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.AssistantJobEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssistantJobRepository extends JpaRepository<AssistantJobEntity, UUID> {

    Optional<AssistantJobEntity> findByInputMessageId(UUID inputMessageId);

    @Query(value = """
            WITH due_jobs AS (
                SELECT job_id
                FROM ai.assistant_jobs
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
            UPDATE ai.assistant_jobs job
            SET status = 'PROCESSING',
                attempt_count = job.attempt_count + 1,
                locked_at = :now,
                locked_by = :lockedBy,
                lock_expires_at = :lockExpiresAt,
                error_code = NULL
            FROM due_jobs
            WHERE job.job_id = due_jobs.job_id
            RETURNING job.*
            """, nativeQuery = true)
    List<AssistantJobEntity> claimDueJobs(
            @Param("now") Instant now,
            @Param("lockExpiresAt") Instant lockExpiresAt,
            @Param("lockedBy") String lockedBy,
            @Param("limit") int limit
    );
}
