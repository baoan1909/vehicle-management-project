package com.ban.vehicle_management.domain.ai.model;

public record AiResponse(
        boolean success,
        boolean retryable,
        String text,
        String rawBody,
        Integer inputTokens,
        Integer outputTokens,
        String failureCode
) {

    public static AiResponse success(String text, String rawBody, Integer inputTokens, Integer outputTokens) {
        return new AiResponse(true, false, text, rawBody, inputTokens, outputTokens, null);
    }

    public static AiResponse failure(String failureCode, boolean retryable, String rawBody) {
        return new AiResponse(false, retryable, null, rawBody, null, null, failureCode);
    }
}
