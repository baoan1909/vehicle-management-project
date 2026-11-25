package com.ban.vehicle_management.domain.ai.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Reviewed FAQ evidence; document publication is the approval boundary, independent of model indexes. */
public record CommonKnowledgeCandidate(UUID documentId, UUID chunkId, String title, String content,
        String summary, Integer sourcePage, String sourceSection, BigDecimal score,
        Integer documentVersion, Instant reviewedAt, String documentChecksum) {
    public KnowledgeSearchResult evidence() {
        return new KnowledgeSearchResult(documentId, chunkId, title, content, summary, sourcePage, sourceSection, score);
    }
}
