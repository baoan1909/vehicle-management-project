package com.ban.vehicle_management.infrastructure.persistence.adapter.ai;

import com.ban.vehicle_management.application.ai.port.out.KnowledgeRetrievalPortOut;
import com.ban.vehicle_management.domain.ai.model.EmbeddingVector;
import com.ban.vehicle_management.domain.ai.model.KnowledgeSearchResult;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeChunkRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeRetrievalPersistenceAdapter implements KnowledgeRetrievalPortOut {

    private final KnowledgeChunkRepository repository;

    public KnowledgeRetrievalPersistenceAdapter(KnowledgeChunkRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<KnowledgeSearchResult> search(UUID tenantId, String query, List<String> accessScopes, int limit) {
        return repository.hybridSearch(tenantId, query, accessScopes, limit);
    }

    @Override
    public List<KnowledgeSearchResult> searchVector(
            UUID tenantId,
            EmbeddingVector queryVector,
            List<String> accessScopes,
            UUID indexVersionId,
            int limit
    ) {
        return repository.vectorSearch(tenantId, queryVector, accessScopes, indexVersionId, limit);
    }
}
