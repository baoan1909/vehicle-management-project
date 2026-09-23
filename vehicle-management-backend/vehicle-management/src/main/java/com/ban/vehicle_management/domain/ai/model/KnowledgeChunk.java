package com.ban.vehicle_management.domain.ai.model;

import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import java.util.UUID;

/**
 * Lightweight projection of an eligible knowledge chunk used while building an index.
 */
public record KnowledgeChunk(
        UUID chunkId,
        String title,
        String content,
        String summary,
        Integer documentVersion
) {
}