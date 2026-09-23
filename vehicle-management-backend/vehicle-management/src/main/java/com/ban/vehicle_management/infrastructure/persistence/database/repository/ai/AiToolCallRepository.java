package com.ban.vehicle_management.infrastructure.persistence.database.repository.ai;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.AiToolCallEntity;
import com.ban.vehicle_management.shared.enumeration.ai.AiToolCallStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiToolCallRepository extends JpaRepository<AiToolCallEntity, UUID> {

    List<AiToolCallEntity> findByInputMessageIdOrderByCreatedAtDesc(UUID inputMessageId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select call from AiToolCallEntity call where call.toolCallId = :toolCallId")
    Optional<AiToolCallEntity> findByIdForUpdate(@Param("toolCallId") UUID toolCallId);

    List<AiToolCallEntity> findByStatusAndExpiresAtBefore(AiToolCallStatus status, Instant now);
}
