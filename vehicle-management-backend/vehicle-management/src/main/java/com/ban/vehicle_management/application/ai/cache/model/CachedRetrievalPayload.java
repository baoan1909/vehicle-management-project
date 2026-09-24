package com.ban.vehicle_management.application.ai.cache.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Cache-safe retrieval evidence. Intentionally excludes runId,
 * conversationId, inputMessageId, requestedBy and old audit identifiers.
 */
public record CachedRetrievalPayload(
        int schemaVersion,
        Instant createdAt,
        UUID activeIndexVersionId,
        String contentChecksum,
        String retrievalPolicyVersion,
        String thresholdVersion,
        String scopeFingerprint,
        String tenantFingerprint,
        int topK,
        List<CachedHybridRow> rows,
        double groundedConfidence,
        boolean evidenceSufficient,
        boolean negative,
        String diagnosticCode) {
}
