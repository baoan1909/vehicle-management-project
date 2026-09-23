package com.ban.vehicle_management.infrastructure.persistence.database.repository.ai;

import com.ban.vehicle_management.domain.ai.model.EmbeddingVector;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class KnowledgeEmbeddingRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public void insertEmbedding(
            UUID chunkId,
            UUID indexVersionId,
            EmbeddingVector vector,
            String modelId,
            int dimension,
            int documentVersion,
            Instant createdAt
    ) {
        String vectorLiteral = toVectorLiteral(vector);
        entityManager.createNativeQuery("""
                        INSERT INTO ai.knowledge_embeddings (
                            chunk_id,
                            index_version_id,
                            embedding,
                            embedding_model,
                            embedding_dimension,
                            document_version,
                            created_at
                        ) VALUES (
                            :chunkId,
                            :indexVersionId,
                            CAST(:embedding AS vector),
                            :modelId,
                            :dimension,
                            :documentVersion,
                            :createdAt
                        )
                        ON CONFLICT (chunk_id, index_version_id) DO NOTHING
                        """)
                .setParameter("chunkId", chunkId)
                .setParameter("indexVersionId", indexVersionId)
                .setParameter("embedding", vectorLiteral)
                .setParameter("modelId", modelId)
                .setParameter("dimension", dimension)
                .setParameter("documentVersion", documentVersion)
                .setParameter("createdAt", createdAt)
                .executeUpdate();
    }

    public long countEmbeddedByIndexVersion(UUID indexVersionId) {
        Object result = entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM ai.knowledge_embeddings
                WHERE index_version_id = :indexVersionId
                """)
                .setParameter("indexVersionId", indexVersionId)
                .getSingleResult();
        return ((Number) result).longValue();
    }

    public long countInvalidVectors(UUID indexVersionId) {
        Object result = entityManager.createNativeQuery("""
                SELECT COUNT(*)
                FROM ai.knowledge_embeddings embedding
                JOIN ai.knowledge_index_versions index_version
                  ON index_version.index_version_id = embedding.index_version_id
                WHERE embedding.index_version_id = :indexVersionId
                  AND (embedding.embedding_dimension <> 768
                       OR vector_dims(embedding.embedding) <> embedding.embedding_dimension
                       OR vector_norm(embedding.embedding) = 0
                       OR embedding.embedding_model <> index_version.model_id
                       OR embedding.embedding_dimension <> index_version.dimension
                       OR NOT EXISTS (
                           SELECT 1
                           FROM ai.knowledge_chunks chunk
                           WHERE chunk.chunk_id = embedding.chunk_id
                       ))
                """)
                .setParameter("indexVersionId", indexVersionId)
                .getSingleResult();
        return ((Number) result).longValue();
    }

    public static String toVectorLiteral(EmbeddingVector vector) {
        double[] values = vector.values();
        StringBuilder literal = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                literal.append(',');
            }
            literal.append(values[i]);
        }
        return literal.append(']').toString();
    }
}
