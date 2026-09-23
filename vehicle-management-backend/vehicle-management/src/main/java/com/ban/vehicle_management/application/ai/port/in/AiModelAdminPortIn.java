package com.ban.vehicle_management.application.ai.port.in;

import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.domain.ai.model.AiModelWarning;
import com.ban.vehicle_management.domain.ai.model.AiProviderModel;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus;
import java.util.List;
import java.util.UUID;

public interface AiModelAdminPortIn {

    List<AiModelConfiguration> listConfigurations();

    AiModelConfiguration updateStatus(UUID configurationId, AiModelStatus status);

    AiModelConfiguration updateRollout(UUID configurationId, int rolloutPercentage);

    List<AiProviderModel> listCatalog();

    List<AiProviderModel> syncCatalog();

    List<AiModelWarning> listWarnings();
}
