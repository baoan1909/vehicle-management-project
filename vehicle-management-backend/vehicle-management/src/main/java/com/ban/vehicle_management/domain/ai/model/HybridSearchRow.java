package com.ban.vehicle_management.domain.ai.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One fused hybrid retrieval row. Ranks and branch scores are kept for audit;
 * {@link KnowledgeSearchResult} carries the user-facing projection.
 */
public record HybridSearchRow(
        UUID documentId,
        UUID chunkId,
        String title,
        String content,
        String summary,
        Integer sourcePage,
        String sourceSection,
        Integer vectorRank,
        Integer lexicalRank,
        BigDecimal vectorScore,
        BigDecimal lexicalScore,
        BigDecimal fusedScore,
        int finalRank
) {
}
