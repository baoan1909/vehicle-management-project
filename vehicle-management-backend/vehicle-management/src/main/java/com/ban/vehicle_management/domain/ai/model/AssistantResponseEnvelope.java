package com.ban.vehicle_management.domain.ai.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Backend-owned response contract. The model may propose text, but citations,
 * action cards, confidence and handoff are always decided by the backend.
 */
public record AssistantResponseEnvelope(
        String responseText,
        List<AssistantCitation> citations,
        List<AssistantActionCard> actionCards,
        double confidence,
        boolean handoffRecommended
) {
    public record AssistantCitation(
            String label,
            UUID chunkId,
            UUID documentId,
            String title,
            Integer page,
            String section
    ) {
    }

    public record AssistantActionCard(
            UUID toolCallId,
            String toolName,
            String title,
            String description,
            Instant expiresAt
    ) {
    }
}
