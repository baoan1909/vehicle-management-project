package com.ban.vehicle_management.infrastructure.persistence.database.repository.ai;

import com.ban.vehicle_management.domain.ai.model.EmbeddingVector;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.KnowledgeEmbeddingEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KnowledgeEmbeddingRepository extends JpaRepository<KnowledgeEmbeddingEntity, UUID> {

    @Query(value = "select extversion from pg_extension where extname='vector'", nativeQuery = true)
    String findPgVectorExtensionVersion();

    @Query(value = """
        select format_type(attribute.atttypid, attribute.atttypmod)
        from pg_attribute attribute
        join pg_class relation on relation.oid=attribute.attrelid
        join pg_namespace namespace on namespace.oid=relation.relnamespace
        where namespace.nspname='ai' and relation.relname='knowledge_embeddings'
          and attribute.attname='embedding' and not attribute.attisdropped
        limit 1
        """, nativeQuery = true)
    String findEmbeddingColumnType();
    default void insertEmbedding(UUID chunkId, UUID indexVersionId, EmbeddingVector vector,
            String modelId, int dimension, int documentVersion, Instant createdAt) {
        insertEmbeddingLiteral(chunkId, indexVersionId, toVectorLiteral(vector), modelId,
                dimension, documentVersion, createdAt);
    }

    @Modifying
    @Query(value = """
        insert into ai.knowledge_embeddings (
            chunk_id, index_version_id, embedding, embedding_model,
            embedding_dimension, document_version, created_at
        ) values (
            :chunkId, :indexVersionId, cast(:embedding as vector), :modelId,
            :dimension, :documentVersion, :createdAt
        ) on conflict (chunk_id, index_version_id) do nothing
        """, nativeQuery = true)
    int insertEmbeddingLiteral(@Param("chunkId") UUID chunkId,
            @Param("indexVersionId") UUID indexVersionId,
            @Param("embedding") String embedding,
            @Param("modelId") String modelId,
            @Param("dimension") int dimension,
            @Param("documentVersion") int documentVersion,
            @Param("createdAt") Instant createdAt);

    @Query(value = "select count(*) from ai.knowledge_embeddings where index_version_id=:indexVersionId",
            nativeQuery = true)
    long countEmbeddedByIndexVersion(@Param("indexVersionId") UUID indexVersionId);

    @Query(value = """
        select count(*) from ai.knowledge_embeddings embedding
        join ai.knowledge_index_versions index_version
          on index_version.index_version_id = embedding.index_version_id
        where embedding.index_version_id = :indexVersionId
          and (embedding.embedding_dimension <> 768
               or vector_dims(embedding.embedding) <> embedding.embedding_dimension
               or vector_norm(embedding.embedding) = 0
               or embedding.embedding_model <> index_version.model_id
               or embedding.embedding_dimension <> index_version.dimension
               or not exists (select 1 from ai.knowledge_chunks chunk
                              where chunk.chunk_id = embedding.chunk_id))
        """, nativeQuery = true)
    long countInvalidVectors(@Param("indexVersionId") UUID indexVersionId);

    static String toVectorLiteral(EmbeddingVector vector) {
        double[] values = vector.values();
        StringBuilder literal = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) literal.append(',');
            literal.append(values[i]);
        }
        return literal.append(']').toString();
    }
}
