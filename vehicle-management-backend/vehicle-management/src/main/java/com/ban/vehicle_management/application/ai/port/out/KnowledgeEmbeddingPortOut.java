package com.ban.vehicle_management.application.ai.port.out;

import com.ban.vehicle_management.domain.ai.model.EmbeddingVector;
import java.time.Instant;
import java.util.UUID;

/**
 * Raw embedding row persistence. Insertion is idempotent per (chunk, index version).
 */
public interface KnowledgeEmbeddingPortOut {

    long countEmbeddedByIndexVersion(UUID indexVersionId);

    long countInvalidVectors(UUID indexVersionId);

    void insertEmbedding(
            UUID chunkId,
            UUID indexVersionId,
            EmbeddingVector vector,
            String modelId,
            int dimension,
            int documentVersion,
            Instant createdAt
    );
}