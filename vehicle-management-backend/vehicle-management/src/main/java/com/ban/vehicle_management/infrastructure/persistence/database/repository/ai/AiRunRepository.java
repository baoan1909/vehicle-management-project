package com.ban.vehicle_management.infrastructure.persistence.database.repository.ai;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.AiRunEntity;
import com.ban.vehicle_management.shared.enumeration.ai.AiRunStatus;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiRunRepository extends JpaRepository<AiRunEntity, UUID> {

    boolean existsByInputMessageIdAndStatus(UUID inputMessageId, AiRunStatus status);
}
