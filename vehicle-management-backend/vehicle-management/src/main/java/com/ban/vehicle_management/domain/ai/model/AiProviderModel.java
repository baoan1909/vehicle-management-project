package com.ban.vehicle_management.domain.ai.model;

import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import java.util.List;

public record AiProviderModel(
        AiProvider provider,
        String modelId,
        String displayName,
        String modelVersion,
        Integer inputTokenLimit,
        Integer outputTokenLimit,
        List<String> supportedActions
) {
}
