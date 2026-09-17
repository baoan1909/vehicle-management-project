package com.ban.vehicle_management.domain.ai.model;

import java.util.List;
import java.util.UUID;

/**
 * Retrieval output that also reports WHY no results were produced. The assistant uses
 * the diagnostic to fall back to a "chưa đủ dữ liệu + tạo phiếu hỗ trợ" response, and
 * the admin retrieval-test screen shows it directly.
 */
public record KnowledgeRetrievalResult(
        String diagnosticCode,
        UUID activeIndexVersionId,
        List<KnowledgeSearchResult> results
) {
    public static KnowledgeRetrievalResult ok(UUID activeIndexVersionId, List<KnowledgeSearchResult> results) {
        return new KnowledgeRetrievalResult(null, activeIndexVersionId, results);
    }

    public static KnowledgeRetrievalResult empty(String diagnosticCode) {
        return new KnowledgeRetrievalResult(diagnosticCode, null, List.of());
    }

    public boolean hasResults() {
        return results != null && !results.isEmpty();
    }
}