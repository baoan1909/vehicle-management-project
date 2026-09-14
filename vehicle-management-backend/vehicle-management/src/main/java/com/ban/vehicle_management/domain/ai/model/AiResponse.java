package com.ban.vehicle_management.domain.ai.model;

public record AiResponse(
        boolean success,
        boolean retryable,
        String text,
        String rawBody,
        Integer inputTokens,
        Integer outputTokens,
        String failureCode,
        AiFunctionCall functionCall,
        String finishReason,
        boolean safetyBlocked,
        AiProviderErrorDetail providerErrorDetail
) {

    public static AiResponse success(String text, String rawBody, Integer inputTokens, Integer outputTokens) {
        return new AiResponse(true, false, text, rawBody, inputTokens, outputTokens, null, null, null, false, null);
    }

    public static AiResponse functionCall(AiFunctionCall functionCall, String rawBody, Integer inputTokens, Integer outputTokens, String finishReason) {
        return new AiResponse(true, false, null, rawBody, inputTokens, outputTokens, null, functionCall, finishReason, false, null);
    }

    public static AiResponse failure(String failureCode, boolean retryable, String rawBody) {
        return failure(failureCode, retryable, rawBody, null);
    }

    public static AiResponse failure(String failureCode, boolean retryable, String rawBody, AiProviderErrorDetail providerErrorDetail) {
        return new AiResponse(false, retryable, null, rawBody, null, null, failureCode, null, null, false, providerErrorDetail);
    }

    public static AiResponse safetyBlocked(String rawBody, Integer inputTokens, Integer outputTokens) {
        return new AiResponse(false, false, null, rawBody, inputTokens, outputTokens, "SAFETY_BLOCKED", null, "SAFETY", true, null);
    }
}
