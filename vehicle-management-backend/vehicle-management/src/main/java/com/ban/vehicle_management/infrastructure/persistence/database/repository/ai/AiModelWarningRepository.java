package com.ban.vehicle_management.infrastructure.persistence.database.repository.ai;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.AiModelWarningEntity;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelWarningStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiModelWarningRepository extends JpaRepository<AiModelWarningEntity, UUID> {

    List<AiModelWarningEntity> findByStatusInOrderByDetectedAtDesc(Collection<AiModelWarningStatus> statuses);

    Optional<AiModelWarningEntity> findFirstByProviderAndModelIdAndConfigurationIdAndWarningCodeAndStatusIn(
            AiProvider provider,
            String modelId,
            UUID configurationId,
            String warningCode,
            Collection<AiModelWarningStatus> statuses
    );
}
