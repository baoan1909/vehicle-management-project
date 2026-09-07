package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.application.ai.port.out.AiModelConfigurationPortOut;
import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus;
import com.ban.vehicle_management.shared.enumeration.ai.AiUseCase;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AiModelRouter {

    private final AiModelConfigurationPortOut modelConfigurationPortOut;
    private final AiModelPolicyService policyService;

    public AiModelRouter(
            AiModelConfigurationPortOut modelConfigurationPortOut,
            AiModelPolicyService policyService
    ) {
        this.modelConfigurationPortOut = modelConfigurationPortOut;
        this.policyService = policyService;
    }

    public List<AiModelConfiguration> resolveCandidates(AiUseCase useCase) {
        List<AiModelConfiguration> candidates = modelConfigurationPortOut.findEnabledByUseCase(useCase).stream()
                .filter(policyService::canUse)
                .sorted(Comparator
                        .comparing((AiModelConfiguration config) -> config.getStatus() == AiModelStatus.ACTIVE ? 0 : 1)
                        .thenComparing(config -> config.getPriority() == null ? Integer.MAX_VALUE : config.getPriority()))
                .toList();
        if (candidates.isEmpty()) {
            throw new BadRequestException("No AI model configuration is enabled for " + useCase);
        }
        return candidates;
    }
}
