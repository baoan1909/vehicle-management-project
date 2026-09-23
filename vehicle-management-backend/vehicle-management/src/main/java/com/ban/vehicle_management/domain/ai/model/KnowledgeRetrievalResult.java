package com.ban.vehicle_management.domain.ai.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Retrieval output that also reports WHY no results were produced. The assistant uses
 * the diagnostic to fall back to a "chưa đủ dữ liệu + tạo phiếu hỗ trợ" response, and
 * the admin retrieval-test screen shows it directly.
 *
 * <p>Confidence is computed by {@link GroundedConfidenceCalculator} from normalized
 * evidence signals — never a raw RRF or cosine value compared against business
 * thresholds.</p>
 */
public record KnowledgeRetrievalResult(
        String diagnosticCode,
        UUID activeIndexVersionId,
        List<KnowledgeSearchResult> results,
        UUID retrievalAuditId,
        BigDecimal groundedConfidence,
        boolean evidenceSufficient,
        String normalizedQuery,
        String retrievalPolicyVersion
) {
    public static KnowledgeRetrievalResult ok(UUID activeIndexVersionId, List<KnowledgeSearchResult> results) {
        return new KnowledgeRetrievalResult(null, activeIndexVersionId, results,
                null, null, !results.isEmpty(), null, null);
    }

    public static KnowledgeRetrievalResult empty(String diagnosticCode) {
        return new KnowledgeRetrievalResult(diagnosticCode, null, List.of(),
                null, null, false, null, null);
    }

    public boolean hasResults() {
        return results != null && !results.isEmpty();
    }

    /** Backend-assigned citation labels (C1, C2, …) in final-rank order. */
    public Map<UUID, String> citationLabels() {
        if (results == null) {
            return Map.of();
        }
        return IntStream.range(0, results.size())
                .boxed()
                .collect(Collectors.toMap(
                        index -> results.get(index).chunkId(),
                        index -> "C" + (index + 1),
                        (first, second) -> first));
    }
}
