package com.ban.vehicle_management.domain.ai.model;

/**
 * Embedding failure details. providerErrorCode and providerErrorMessage must already be
 * redacted by the provider layer before the domain sees them.
 */
public record EmbeddingFailure(
        String code,
        Integer providerStatus,
        String providerErrorCode,
        String providerErrorMessageRedacted,
        boolean retryable,
        Long retryAfterSeconds
) {
}