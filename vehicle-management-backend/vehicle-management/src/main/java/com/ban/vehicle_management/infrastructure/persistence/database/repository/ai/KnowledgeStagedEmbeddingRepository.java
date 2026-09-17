package com.ban.vehicle_management.infrastructure.persistence.database.repository.ai;

import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeStagedEmbedding;
import com.ban.vehicle_management.domain.ai.model.EmbeddingVector;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Native persistence for pre-review embeddings. The staged table carries a pgvector
 * column which has no Hibernate type mapping, so inserts go through the same vector
 * literal helper used by the final embeddings table.
 */
@Repository
public class KnowledgeStagedEmbeddingRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void replaceForChunk(UUID chunkId, List<KnowledgeStagedEmbedding> staged) {
        deleteByChunkIds(List.of(chunkId));
        for (KnowledgeStagedEmbedding embedding : staged) {
            entityManager.createNativeQuery("""
                            INSERT INTO ai.knowledge_staged_embeddings (
                                chunk_id,
                                provider,
                                model_configuration_id,
                                model_id,
                                embedding_dimension,
                                embedding_prompt_version,
                                chunker_version,
                                content_hash,
                                embedding_fingerprint,
                                embedding,
                                created_at
                            ) VALUES (
                                :chunkId,
                                :provider,
                                :modelConfigurationId,
                                :modelId,
                                :dimension,
                                :promptVersion,
                                :chunkerVersion,
                                :contentHash,
                                :fingerprint,
                                CAST(:embedding AS vector),
                                :createdAt
                            )
                            ON CONFLICT (chunk_id, embedding_fingerprint) DO NOTHING
                            """)
                    .setParameter("chunkId", chunkId)
                    .setParameter("provider", embedding.getProvider().name())
                    .setParameter("modelConfigurationId", embedding.getModelConfigurationId())
                    .setParameter("modelId", embedding.getModelId())
                    .setParameter("dimension", embedding.getEmbeddingDimension())
                    .setParameter("promptVersion", embedding.getEmbeddingPromptVersion())
                    .setParameter("chunkerVersion", embedding.getChunkerVersion())
                    .setParameter("contentHash", embedding.getContentHash())
                    .setParameter("fingerprint", embedding.getEmbeddingFingerprint())
                    .setParameter("embedding", toVectorLiteral(embedding.getEmbedding()))
                    .setParameter("createdAt", Timestamp.from(embedding.getCreatedAt()))
                    .executeUpdate();
        }
    }

    @Transactional
    public void deleteByChunkIds(Collection<UUID> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return;
        }
        entityManager.createNativeQuery("""
                        DELETE FROM ai.knowledge_staged_embeddings
                        WHERE chunk_id IN (:chunkIds)
                        """)
                .setParameter("chunkIds", chunkIds)
                .executeUpdate();
    }

    @SuppressWarnings("unchecked")
    public List<KnowledgeStagedEmbedding> findByChunkIds(Collection<UUID> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return List.of();
        }
        List<Object[]> rows = entityManager.createNativeQuery("""
                        SELECT staged.staged_embedding_id,
                               staged.chunk_id,
                               staged.provider,
                               staged.model_configuration_id,
                               staged.model_id,
                               staged.embedding_dimension,
                               staged.embedding_prompt_version,
                               staged.chunker_version,
                               staged.content_hash,
                               staged.embedding_fingerprint,
                               staged.embedding::float8[],
                               staged.created_at
                        FROM ai.knowledge_staged_embeddings staged
                        WHERE staged.chunk_id IN (:chunkIds)
                        """)
                .setParameter("chunkIds", chunkIds)
                .getResultList();
        List<KnowledgeStagedEmbedding> result = new ArrayList<>();
        for (Object[] row : rows) {
            KnowledgeStagedEmbedding staged = new KnowledgeStagedEmbedding();
            staged.setStagedEmbeddingId((UUID) row[0]);
            staged.setChunkId((UUID) row[1]);
            staged.setProvider(com.ban.vehicle_management.shared.enumeration.ai.AiProvider.valueOf((String) row[2]));
            staged.setModelConfigurationId((UUID) row[3]);
            staged.setModelId((String) row[4]);
            staged.setEmbeddingDimension(((Number) row[5]).intValue());
            staged.setEmbeddingPromptVersion((String) row[6]);
            staged.setChunkerVersion((String) row[7]);
            staged.setContentHash((String) row[8]);
            staged.setEmbeddingFingerprint((String) row[9]);
            staged.setEmbedding(toEmbeddingVector((Object[]) row[10], staged.getEmbeddingDimension()));
            staged.setCreatedAt(toInstant(row[11]));
            result.add(staged);
        }
        return result;
    }

    public long countByChunkId(UUID chunkId) {
        Object result = entityManager.createNativeQuery("""
                        SELECT COUNT(*)
                        FROM ai.knowledge_staged_embeddings
                        WHERE chunk_id = :chunkId
                        """)
                .setParameter("chunkId", chunkId)
                .getSingleResult();
        return ((Number) result).longValue();
    }

    private EmbeddingVector toEmbeddingVector(Object[] values, int dimension) {
        double[] vector = new double[values.length];
        for (int index = 0; index < values.length; index++) {
            vector[index] = ((Number) values[index]).doubleValue();
        }
        return EmbeddingVector.of(vector, Math.max(dimension, vector.length));
    }

    private java.time.Instant toInstant(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof java.util.Date date) {
            return date.toInstant();
        }
        return java.time.Instant.now();
    }

    private String toVectorLiteral(EmbeddingVector vector) {
        double[] values = vector.values();
        StringBuilder literal = new StringBuilder("[");
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                literal.append(',');
            }
            literal.append(values[index]);
        }
        return literal.append(']').toString();
    }
}