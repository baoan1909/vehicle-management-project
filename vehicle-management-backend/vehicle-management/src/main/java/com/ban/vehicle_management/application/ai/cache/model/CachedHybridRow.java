package com.ban.vehicle_management.application.ai.cache.model;

import java.math.BigDecimal;
import java.util.UUID;

public record CachedHybridRow(
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
        int finalRank) {
}
