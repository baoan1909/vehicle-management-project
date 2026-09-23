package com.ban.vehicle_management.application.ai.port.out;

import com.ban.vehicle_management.domain.ai.model.AiModelWarning;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelWarningStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiModelWarningPortOut {

    AiModelWarning save(AiModelWarning warning);

    List<AiModelWarning> findByStatuses(Collection<AiModelWarningStatus> statuses);

    Optional<AiModelWarning> findOpen(String provider, String modelId, UUID configurationId, String warningCode);
}
