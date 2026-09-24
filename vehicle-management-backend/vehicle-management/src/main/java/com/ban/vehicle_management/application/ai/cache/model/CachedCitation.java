package com.ban.vehicle_management.application.ai.cache.model;

import java.math.BigDecimal;
import java.util.UUID;

public record CachedCitation(
        UUID documentId,
        UUID chunkId,
        String label,
        String title,
        Integer sourcePage,
        String sourceSection,
        BigDecimal retrievalScore) {
}
