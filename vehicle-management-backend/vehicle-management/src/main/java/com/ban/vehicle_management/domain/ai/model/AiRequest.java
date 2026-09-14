package com.ban.vehicle_management.domain.ai.model;

import java.util.List;

public record AiRequest(
        String systemInstruction,
        List<AiRequestMessage> messages,
        boolean structuredOutput,
        List<AiToolDeclaration> tools,
        List<AiFunctionResponse> functionResponses
) {
    public AiRequest(String systemInstruction, List<AiRequestMessage> messages, boolean structuredOutput) {
        this(systemInstruction, messages, structuredOutput, List.of(), List.of());
    }
}
