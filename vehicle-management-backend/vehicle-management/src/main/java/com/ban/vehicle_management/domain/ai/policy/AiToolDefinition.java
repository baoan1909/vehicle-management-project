package com.ban.vehicle_management.domain.ai.policy;

import com.ban.vehicle_management.domain.ai.model.AiToolDeclaration;
import com.ban.vehicle_management.shared.enumeration.ai.AiToolType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.function.Consumer;

public record AiToolDefinition(
        String name,
        String description,
        String parametersSchema,
        AiToolType toolType,
        String requiredPermission,
        boolean requiresConfirmation,
        Duration timeout,
        String idempotencyScope,
        Consumer<JsonNode> validator
) {
    public AiToolDeclaration declaration() {
        return new AiToolDeclaration(
                name,
                description,
                parametersSchema,
                toolType,
                requiredPermission,
                requiresConfirmation
        );
    }

    public void validate(JsonNode arguments) {
        if (arguments == null || !arguments.isObject()) {
            throw new BadRequestException("AI tool arguments must be a JSON object");
        }
        validator.accept(arguments);
    }
}
