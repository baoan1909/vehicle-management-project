package com.ban.vehicle_management.infrastructure.persistence.adapter.ai;

import com.ban.vehicle_management.application.ai.port.out.KnowledgeChunkPortOut;
import com.ban.vehicle_management.domain.ai.model.KnowledgeChunk;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeChunkRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeChunkPersistenceAdapter implements KnowledgeChunkPortOut {

    private final KnowledgeChunkRepository repository;

    public KnowledgeChunkPersistenceAdapter(KnowledgeChunkRepository repository) {
        this.repository = repository;
    }

    @Override
    public long countEligibleChunks() {
        return repository.countEligibleChunks();
    }

    @Override
    public String calculateEligibleCorpusChecksum() {
        return repository.calculateEligibleCorpusChecksum();
    }

    @Override
    public List<KnowledgeChunk> findEligibleChunks(UUID indexVersionId, int limit) {
        return repository.findEligibleChunks(indexVersionId, limit);
    }
}
