package com.ban.vehicle_management.infrastructure.persistence.database.repository.ai;

import com.ban.vehicle_management.domain.ai.model.KnowledgeSearchResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class KnowledgeChunkRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<KnowledgeSearchResult> hybridSearch(UUID tenantId, String query, List<String> accessScopes, int limit) {
        String sql = """
                SELECT document.document_id,
                       chunk.chunk_id,
                       chunk.title,
                       chunk.content,
                       chunk.summary,
                       chunk.source_page,
                       chunk.source_section,
                       ts_rank(chunk.search_vector, plainto_tsquery('simple', :query)) AS score
                FROM ai.knowledge_chunks chunk
                JOIN ai.knowledge_documents document ON document.document_id = chunk.document_id
                JOIN ai.knowledge_sources source ON source.source_id = document.source_id
                WHERE chunk.status = 'READY'
                  AND document.status = 'READY'
                  AND source.status = 'ACTIVE'
                  AND chunk.access_scope IN (:accessScopes)
                  AND (:tenantId IS NULL OR chunk.tenant_id IS NULL OR chunk.tenant_id = :tenantId)
                  AND chunk.search_vector @@ plainto_tsquery('simple', :query)
                ORDER BY score DESC, chunk.created_at DESC
                LIMIT :limit
                """;
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(sql)
                .setParameter("tenantId", tenantId)
                .setParameter("query", query)
                .setParameter("accessScopes", accessScopes)
                .setParameter("limit", limit)
                .getResultList();
        return rows.stream()
                .map(row -> new KnowledgeSearchResult(
                        (UUID) row[0],
                        (UUID) row[1],
                        (String) row[2],
                        (String) row[3],
                        (String) row[4],
                        (Integer) row[5],
                        (String) row[6],
                        row[7] instanceof BigDecimal value ? value : BigDecimal.ZERO
                ))
                .toList();
    }
}
