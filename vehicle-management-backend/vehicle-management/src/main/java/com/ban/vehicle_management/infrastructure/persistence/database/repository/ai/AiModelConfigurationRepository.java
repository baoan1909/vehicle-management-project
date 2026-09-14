package com.ban.vehicle_management.infrastructure.persistence.database.repository.ai;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.AiModelConfigurationEntity;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiUseCase;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiModelConfigurationRepository extends JpaRepository<AiModelConfigurationEntity, UUID> {

    List<AiModelConfigurationEntity> findByUseCaseAndStatusInOrderByPriorityAscCreatedAtAsc(
            AiUseCase useCase,
            Collection<AiModelStatus> statuses
    );

    List<AiModelConfigurationEntity> findByUseCaseAndStatusOrderByPriorityAscCreatedAtAsc(AiUseCase useCase, AiModelStatus status);
}
