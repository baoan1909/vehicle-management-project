package com.ban.vehicle_management.domain.ai.model;

import java.util.List;

public record AiRequest(
        String systemInstruction,
        List<AiRequestMessage> messages,
        boolean structuredOutput
) {
}
