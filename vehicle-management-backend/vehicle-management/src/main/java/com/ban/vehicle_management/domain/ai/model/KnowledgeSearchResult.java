package com.ban.vehicle_management.domain.ai.model;

import java.math.BigDecimal;
import java.util.UUID;

public record KnowledgeSearchResult(
        UUID documentId,
        UUID chunkId,
        String title,
        String content,
        String summary,
        Integer sourcePage,
        String sourceSection,
        BigDecimal score
) {
}
