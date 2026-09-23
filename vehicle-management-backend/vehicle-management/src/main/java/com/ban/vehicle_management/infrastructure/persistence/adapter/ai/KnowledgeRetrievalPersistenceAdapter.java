package com.ban.vehicle_management.infrastructure.persistence.adapter.ai;

import com.ban.vehicle_management.application.ai.port.out.KnowledgeRetrievalPortOut;
import com.ban.vehicle_management.domain.ai.model.EmbeddingVector;
import com.ban.vehicle_management.domain.ai.model.HybridSearchRow;
import com.ban.vehicle_management.domain.ai.model.KnowledgeSearchResult;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeChunkRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.KnowledgeEmbeddingRepository;
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
    public List<HybridSearchRow> hybridSearch(
            UUID tenantId,
            UUID indexVersionId,
            EmbeddingVector queryVector,
            String normalizedQuery,
            List<String> accessScopes,
            int vectorCandidateLimit,
            int lexicalCandidateLimit,
            double minimumVectorScore,
            double minimumLexicalScore,
            int rrfRankConstant,
            double weightVector,
            double weightLexical,
            int maxChunksPerDocument,
            int finalTopK
    ) {
        return repository.hybridSearch(
                tenantId,
                indexVersionId,
                KnowledgeEmbeddingRepository.toVectorLiteral(queryVector),
                normalizedQuery,
                accessScopes,
                vectorCandidateLimit,
                lexicalCandidateLimit,
                minimumVectorScore,
                minimumLexicalScore,
                rrfRankConstant,
                weightVector,
                weightLexical,
                maxChunksPerDocument,
                finalTopK);
    }

    @Override
    public List<KnowledgeSearchResult> search(
            UUID tenantId,
            String query,
            List<String> accessScopes,
            UUID indexVersionId,
            int limit
    ) {
        return repository.hybridSearch(tenantId, query, accessScopes, indexVersionId, limit);
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
