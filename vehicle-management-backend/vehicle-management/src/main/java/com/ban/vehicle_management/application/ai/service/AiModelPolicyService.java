package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.domain.ai.model.AiModelConfiguration;
import com.ban.vehicle_management.shared.enumeration.ai.AiDataMode;
import com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus;
import org.springframework.stereotype.Service;

@Service
public class AiModelPolicyService {

    private final AiAssistantProperties properties;

    public AiModelPolicyService(AiAssistantProperties properties) {
        this.properties = properties;
    }

    public boolean canUse(AiModelConfiguration configuration) {
        if (configuration == null || configuration.getStatus() == AiModelStatus.DISABLED) {
            return false;
        }
        if (configuration.isRequiresFunctionCalling()) {
            return false;
        }
        return properties.getDataMode() != AiDataMode.UNPAID || configuration.isFreeTierApproved();
    }

    public boolean canFallbackFor(String failureCode) {
        return "TIMEOUT".equals(failureCode)
                || "HTTP_429".equals(failureCode)
                || "HTTP_5XX".equals(failureCode)
                || "MODEL_NOT_FOUND".equals(failureCode)
                || "INVALID_RESPONSE_SCHEMA".equals(failureCode)
                || "INVALID_FUNCTION_CALL".equals(failureCode)
                || "PROVIDER_EXCEPTION".equals(failureCode);
    }
}
