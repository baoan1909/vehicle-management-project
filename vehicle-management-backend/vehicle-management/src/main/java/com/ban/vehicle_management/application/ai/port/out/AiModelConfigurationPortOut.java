package com.ban.vehicle_management.application.ai.port.out;

import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiUseCase;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiModelConfigurationPortOut {

    List<AiModelConfiguration> findEnabledByUseCase(AiUseCase useCase);

    List<AiModelConfiguration> findAll();

    Optional<AiModelConfiguration> findById(UUID configurationId);

    AiModelConfiguration save(AiModelConfiguration configuration);

    List<AiModelConfiguration> findByUseCaseAndStatus(AiUseCase useCase, AiModelStatus status);
}
