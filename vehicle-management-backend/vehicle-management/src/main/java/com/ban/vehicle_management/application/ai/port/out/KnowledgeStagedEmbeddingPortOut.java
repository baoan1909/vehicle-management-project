package com.ban.vehicle_management.application.ai.port.out;

import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeStagedEmbedding;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface KnowledgeStagedEmbeddingPortOut {

    void replaceForChunk(UUID chunkId, List<KnowledgeStagedEmbedding> staged);

    List<KnowledgeStagedEmbedding> findByChunkIds(Collection<UUID> chunkIds);

    void deleteByChunkIds(Collection<UUID> chunkIds);

    long countByChunkId(UUID chunkId);
}