package com.ban.vehicle_management.infrastructure.persistence.adapter.ai;

import com.ban.vehicle_management.application.ai.port.out.KnowledgeStagedEmbeddingPortOut;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeStagedEmbedding;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeStagedEmbeddingRepository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class KnowledgeStagedEmbeddingPersistenceAdapter implements KnowledgeStagedEmbeddingPortOut {

    private final KnowledgeStagedEmbeddingRepository repository;

    public KnowledgeStagedEmbeddingPersistenceAdapter(KnowledgeStagedEmbeddingRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void replaceForChunk(UUID chunkId, List<KnowledgeStagedEmbedding> staged) {
        repository.replaceForChunk(chunkId, staged);
    }

    @Override
    public List<KnowledgeStagedEmbedding> findByChunkIds(Collection<UUID> chunkIds) {
        return repository.findByChunkIds(chunkIds);
    }

    @Override
    @Transactional
    public void deleteByChunkIds(Collection<UUID> chunkIds) {
        repository.deleteByChunkIds(chunkIds);
    }

    @Override
    public long countByChunkId(UUID chunkId) {
        return repository.countByChunkId(chunkId);
    }
}