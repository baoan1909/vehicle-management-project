package com.ban.vehicle_management.domain.ai.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Persisted retrieval audit. The raw query is never stored; only the redacted
 * original and a SHA-256 hash of the normalized query are kept.
 */
public record RetrievalAudit(
        UUID retrievalAuditId,
        UUID runId,
        UUID conversationId,
        UUID inputMessageId,
        UUID outputMessageId,
        UUID requestedBy,
        UUID tenantId,
        String queryRedacted,
        String normalizedQueryHash,
        UUID activeIndexVersionId,
        String retrievalPolicyVersion,
        String thresholdVersion,
        String accessScopes,
        int vectorCandidateCount,
        int lexicalCandidateCount,
        int fusedResultCount,
        int finalResultCount,
        int topK,
        int maxChunksPerDocument,
        BigDecimal groundedConfidence,
        String diagnosticCode,
        Long latencyMs,
        Instant createdAt,
        List<RetrievalAuditResult> results
) {
    public record RetrievalAuditResult(
            UUID documentId,
            UUID chunkId,
            Integer vectorRank,
            Integer lexicalRank,
            BigDecimal vectorScore,
            BigDecimal lexicalScore,
            BigDecimal fusedScore,
            Integer finalRank,
            boolean selectedForContext
    ) {
    }
}
