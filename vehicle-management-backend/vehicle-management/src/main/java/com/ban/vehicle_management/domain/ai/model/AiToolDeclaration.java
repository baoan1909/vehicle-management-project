package com.ban.vehicle_management.domain.ai.model;

import com.ban.vehicle_management.shared.enumeration.ai.AiToolType;

public record AiToolDeclaration(
        String name,
        String description,
        String parametersSchema,
        AiToolType toolType,
        String requiredPermission,
        boolean requiresConfirmation
) {
}
