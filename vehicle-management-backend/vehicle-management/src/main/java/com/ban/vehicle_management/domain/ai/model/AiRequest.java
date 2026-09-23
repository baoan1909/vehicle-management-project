package com.ban.vehicle_management.domain.ai.model;

import java.util.List;

public record AiRequest(
        String systemInstruction,
        List<AiRequestMessage> messages,
        boolean structuredOutput,
        String responseJsonSchema,
        List<AiToolDeclaration> tools,
        List<AiFunctionResponse> functionResponses,
        List<AiFunctionCall> precedingFunctionCalls
) {
    public AiRequest(String systemInstruction, List<AiRequestMessage> messages, boolean structuredOutput) {
        this(systemInstruction, messages, structuredOutput, null, List.of(), List.of(), List.of());
    }

    public AiRequest(
            String systemInstruction,
            List<AiRequestMessage> messages,
            boolean structuredOutput,
            List<AiToolDeclaration> tools,
            List<AiFunctionResponse> functionResponses
    ) {
        this(systemInstruction, messages, structuredOutput, null, tools, functionResponses, List.of());
    }

    public AiRequest(
            String systemInstruction,
            List<AiRequestMessage> messages,
            boolean structuredOutput,
            List<AiToolDeclaration> tools,
            List<AiFunctionResponse> functionResponses,
            List<AiFunctionCall> precedingFunctionCalls
    ) {
        this(systemInstruction, messages, structuredOutput, null,
                tools, functionResponses, precedingFunctionCalls);
    }
}
