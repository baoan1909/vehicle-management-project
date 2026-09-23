package com.ban.vehicle_management.infrastructure.persistence.database.repository.ai;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.AiRunEntity;
import com.ban.vehicle_management.shared.enumeration.ai.AiRunStatus;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiRunRepository extends JpaRepository<AiRunEntity, UUID> {

    boolean existsByInputMessageIdAndStatus(UUID inputMessageId, AiRunStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE AiRunEntity run
            SET run.status = :failedStatus,
                run.failureCode = :failureCode,
                run.failureRetryable = :retryable
            WHERE run.inputMessageId = :inputMessageId
              AND run.status = :runningStatus
            """)
    int failRunningForInputMessage(
            @Param("inputMessageId") UUID inputMessageId,
            @Param("failureCode") String failureCode,
            @Param("retryable") boolean retryable,
            @Param("runningStatus") AiRunStatus runningStatus,
            @Param("failedStatus") AiRunStatus failedStatus);
}
