package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.domain.ai.policy.AiToolPolicy;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

@Service
public class AiToolExecutionService {

    private final AiToolPolicy toolPolicy = new AiToolPolicy();

    public void validateToolCall(String toolName, JsonNode arguments) {
        if (!toolPolicy.isAllowed(toolName)) {
            throw new BadRequestException("AI tool is not allowed");
        }
        if (arguments != null && !arguments.isObject()) {
            throw new BadRequestException("AI tool arguments must be a JSON object");
        }
    }
}
