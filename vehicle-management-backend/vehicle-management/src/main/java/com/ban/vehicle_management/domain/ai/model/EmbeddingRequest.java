package com.ban.vehicle_management.domain.ai.model;

import java.util.UUID;

/**
 * Input for a single embedding call. Document chunks carry a chunkId; query embedding
 * requests leave it null.
 */
public record EmbeddingRequest(String text, UUID chunkId) {

    public EmbeddingRequest {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Embedding text must not be blank");
        }
    }
}