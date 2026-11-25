package com.ban.vehicle_management.domain.ai.model;

import java.time.Instant;

/** A bounded provider attempt. Credential aliases never contain a secret. */
public record AiProviderAttempt(AiModelConfiguration configuration, String credentialAlias, Instant deadline) {
}
