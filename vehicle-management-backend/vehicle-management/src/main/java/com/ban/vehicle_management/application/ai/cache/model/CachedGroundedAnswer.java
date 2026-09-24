package com.ban.vehicle_management.application.ai.cache.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CachedGroundedAnswer(
        int schemaVersion,
        Instant createdAt,
        String responseText,
        List<CachedCitation> citations,
        UUID activeIndexVersionId,
        String contentChecksum,
        String promptVersion,
        String generationPolicyFingerprint,
        String scopeFingerprint,
        String tenantFingerprint,
        String language,
        double groundedConfidence,
        boolean handoffRecommended) {
}
