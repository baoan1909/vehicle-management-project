package com.ban.vehicle_management.application.ai.port.out;

import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.shared.enumeration.ai.AiUseCase;
import java.util.List;

public interface AiModelConfigurationPortOut {

    List<AiModelConfiguration> findEnabledByUseCase(AiUseCase useCase);
}
