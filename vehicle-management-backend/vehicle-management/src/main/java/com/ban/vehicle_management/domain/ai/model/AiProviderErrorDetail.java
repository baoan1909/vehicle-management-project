package com.ban.vehicle_management.domain.ai.model;

public record AiProviderErrorDetail(
        Integer providerStatus,
        String providerErrorCode,
        String providerErrorMessageRedacted,
        String fieldViolationsRedacted,
        boolean retryable
) {
}
