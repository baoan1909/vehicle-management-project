package com.ban.vehicle_management.infrastructure.persistence.database.repository.ai;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.AiRuntimeModeEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiRuntimeModeRepository extends JpaRepository<AiRuntimeModeEntity, Short> {
    @Query(value = "SELECT * FROM ai.runtime_mode WHERE singleton_id = 1", nativeQuery = true)
    Optional<AiRuntimeModeEntity> findCurrent();

    @Query(value = "SELECT * FROM ai.runtime_mode WHERE singleton_id = 1 FOR SHARE", nativeQuery = true)
    Optional<AiRuntimeModeEntity> findCurrentForPublication();

    @Query(value = "SELECT * FROM ai.runtime_mode WHERE singleton_id = 1 FOR UPDATE", nativeQuery = true)
    Optional<AiRuntimeModeEntity> findCurrentForUpdate();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE ai.runtime_mode
            SET target_mode = :target, epoch = epoch + 1, status = 'SWITCHING',
                changed_by = :actor, changed_at = now()
            WHERE singleton_id = 1 AND epoch = :expectedEpoch AND status <> 'SWITCHING'
            """, nativeQuery = true)
    int requestSwitch(@Param("target") String target, @Param("expectedEpoch") long expectedEpoch,
            @Param("actor") UUID actor);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE ai.runtime_mode SET mode = target_mode, status = :status
            WHERE singleton_id = 1 AND epoch = :epoch AND status = 'SWITCHING'
            """, nativeQuery = true)
    int completeSwitch(@Param("epoch") long epoch, @Param("status") String status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE ai.runtime_mode SET status = 'READY'
            WHERE singleton_id = 1 AND epoch = :epoch AND status = 'PREPARING'
            """, nativeQuery = true)
    int markReady(@Param("epoch") long epoch);

    @Query(value = "SELECT pg_try_advisory_xact_lock_shared(:lockKey)", nativeQuery = true)
    boolean tryAcquireSharedLock(@Param("lockKey") long lockKey);

    @Query(value = "SELECT pg_try_advisory_xact_lock(:lockKey)", nativeQuery = true)
    boolean tryAcquireExclusiveLock(@Param("lockKey") long lockKey);

    @Modifying
    @Query(value = """
            INSERT INTO ai.runtime_mode_events(event_id, from_mode, to_mode, epoch, actor_id, result)
            VALUES(gen_random_uuid(), :fromMode, :toMode, :epoch, :actor, :result)
            """, nativeQuery = true)
    int recordModeEvent(@Param("fromMode") String fromMode, @Param("toMode") String toMode,
            @Param("epoch") long epoch, @Param("actor") UUID actor, @Param("result") String result);

    @Modifying
    @Query(value = """
            INSERT INTO ai.provider_http_calls(call_id, epoch, provider, operation, started_at,
                latency_ms, http_status, failure_code)
            VALUES(gen_random_uuid(), :epoch, :provider, :operation, :startedAt,
                :latencyMs, :status, :failure)
            """, nativeQuery = true)
    int recordHttpCall(@Param("epoch") long epoch, @Param("provider") String provider,
            @Param("operation") String operation, @Param("startedAt") Instant startedAt,
            @Param("latencyMs") long latencyMs, @Param("status") int status,
            @Param("failure") String failure);
}
