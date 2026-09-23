package com.ban.vehicle_management.domain.ai.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Validated citation bound to a persisted assistant message. Labels (C1, C2, …)
 * are assigned by the backend from the retrieval allowlist; the model may only
 * reference labels it was given.
 */
public record AiMessageCitation(
        UUID citationId,
        UUID messageId,
        UUID documentId,
        UUID chunkId,
        String label,
        String title,
        Integer sourcePage,
        String sourceSection,
        BigDecimal retrievalScore,
        UUID retrievalAuditId,
        UUID indexVersionId,
        int citationOrder
) {
}
