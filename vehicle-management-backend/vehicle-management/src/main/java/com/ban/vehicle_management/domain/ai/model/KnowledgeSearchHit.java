package com.ban.vehicle_management.domain.ai.model;

import java.util.List;
import java.util.UUID;

/** Projection of a single retrieval hit used by the retrieval-test screen. */
public record KnowledgeSearchHit(
        UUID documentId,
        UUID chunkId,
        String title,
        String content,
        String sourcePage,
        String sourceSection,
        java.math.BigDecimal score
) {
    public static KnowledgeSearchHit from(KnowledgeSearchResult result) {
        return new KnowledgeSearchHit(
                result.documentId(),
                result.chunkId(),
                result.title(),
                result.content(),
                result.sourcePage() == null ? null : String.valueOf(result.sourcePage()),
                result.sourceSection(),
                result.score());
    }

    public static List<KnowledgeSearchHit> fromAll(List<KnowledgeSearchResult> results) {
        return results == null ? List.of() : results.stream().map(KnowledgeSearchHit::from).toList();
    }
}