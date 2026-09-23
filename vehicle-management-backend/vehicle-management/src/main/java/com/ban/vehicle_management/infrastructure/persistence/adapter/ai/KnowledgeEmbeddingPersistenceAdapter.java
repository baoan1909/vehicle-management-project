package com.ban.vehicle_management.infrastructure.persistence.adapter.ai;

import com.ban.vehicle_management.application.ai.port.out.KnowledgeEmbeddingPortOut;
import com.ban.vehicle_management.domain.ai.model.EmbeddingVector;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeEmbeddingRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class KnowledgeEmbeddingPersistenceAdapter implements KnowledgeEmbeddingPortOut {

    private final KnowledgeEmbeddingRepository repository;

    public KnowledgeEmbeddingPersistenceAdapter(KnowledgeEmbeddingRepository repository) {
        this.repository = repository;
    }

    @Override
    public long countEmbeddedByIndexVersion(UUID indexVersionId) {
        return repository.countEmbeddedByIndexVersion(indexVersionId);
    }

    @Override
    public long countInvalidVectors(UUID indexVersionId) {
        return repository.countInvalidVectors(indexVersionId);
    }

    @Override
    @Transactional
    public void insertEmbedding(
            UUID chunkId,
            UUID indexVersionId,
            EmbeddingVector vector,
            String modelId,
            int dimension,
            int documentVersion,
            Instant createdAt
    ) {
        repository.insertEmbedding(chunkId, indexVersionId, vector, modelId, dimension, documentVersion, createdAt);
    }
}
