package com.ban.vehicle_management.infrastructure.persistence.database.repository.ai;

import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeStagedEmbedding;
import com.ban.vehicle_management.domain.ai.model.EmbeddingVector;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.ai.KnowledgeStagedEmbeddingEntity;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KnowledgeStagedEmbeddingRepository
        extends JpaRepository<KnowledgeStagedEmbeddingEntity, UUID> {

    interface StagedEmbeddingProjection {
        UUID getStagedEmbeddingId();
        UUID getChunkId();
        String getProvider();
        UUID getModelConfigurationId();
        String getModelId();
        Integer getEmbeddingDimension();
        String getEmbeddingPromptVersion();
        String getChunkerVersion();
        String getContentHash();
        String getEmbeddingFingerprint();
        String getEmbeddingLiteral();
        Instant getCreatedAt();
    }

    default void replaceForChunk(UUID chunkId, List<KnowledgeStagedEmbedding> staged) {
        deleteByChunkIds(List.of(chunkId));
        staged.forEach(item -> insertStagedEmbedding(
                chunkId, item.getProvider().name(), item.getModelConfigurationId(), item.getModelId(),
                item.getEmbeddingDimension(), item.getEmbeddingPromptVersion(), item.getChunkerVersion(),
                item.getContentHash(), item.getEmbeddingFingerprint(),
                KnowledgeEmbeddingRepository.toVectorLiteral(item.getEmbedding()), item.getCreatedAt()));
    }

    @Modifying
    @Query(value = """
        insert into ai.knowledge_staged_embeddings (
            chunk_id, provider, model_configuration_id, model_id, embedding_dimension,
            embedding_prompt_version, chunker_version, content_hash,
            embedding_fingerprint, embedding, created_at
        ) values (
            :chunkId, :provider, :modelConfigurationId, :modelId, :dimension,
            :promptVersion, :chunkerVersion, :contentHash,
            :fingerprint, cast(:embedding as vector), :createdAt
        ) on conflict (chunk_id, embedding_fingerprint) do nothing
        """, nativeQuery = true)
    int insertStagedEmbedding(@Param("chunkId") UUID chunkId,
            @Param("provider") String provider,
            @Param("modelConfigurationId") UUID modelConfigurationId,
            @Param("modelId") String modelId,
            @Param("dimension") int dimension,
            @Param("promptVersion") String promptVersion,
            @Param("chunkerVersion") String chunkerVersion,
            @Param("contentHash") String contentHash,
            @Param("fingerprint") String fingerprint,
            @Param("embedding") String embedding,
            @Param("createdAt") Instant createdAt);

    default void deleteByChunkIds(Collection<UUID> chunkIds) {
        if (chunkIds != null && !chunkIds.isEmpty()) deleteExistingByChunkIds(chunkIds);
    }

    @Modifying
    @Query(value = "delete from ai.knowledge_staged_embeddings where chunk_id in (:chunkIds)",
            nativeQuery = true)
    int deleteExistingByChunkIds(@Param("chunkIds") Collection<UUID> chunkIds);

    default List<KnowledgeStagedEmbedding> findByChunkIds(Collection<UUID> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) return List.of();
        return findRowsByChunkIds(chunkIds).stream().map(row -> {
            KnowledgeStagedEmbedding staged = new KnowledgeStagedEmbedding();
            staged.setStagedEmbeddingId(row.getStagedEmbeddingId());
            staged.setChunkId(row.getChunkId());
            staged.setProvider(AiProvider.valueOf(row.getProvider()));
            staged.setModelConfigurationId(row.getModelConfigurationId());
            staged.setModelId(row.getModelId());
            staged.setEmbeddingDimension(row.getEmbeddingDimension());
            staged.setEmbeddingPromptVersion(row.getEmbeddingPromptVersion());
            staged.setChunkerVersion(row.getChunkerVersion());
            staged.setContentHash(row.getContentHash());
            staged.setEmbeddingFingerprint(row.getEmbeddingFingerprint());
            staged.setEmbedding(fromVectorLiteral(row.getEmbeddingLiteral(), row.getEmbeddingDimension()));
            staged.setCreatedAt(row.getCreatedAt());
            return staged;
        }).toList();
    }

    @Query(value = """
        select staged_embedding_id as "stagedEmbeddingId", chunk_id as "chunkId",
               provider as "provider", model_configuration_id as "modelConfigurationId",
               model_id as "modelId", embedding_dimension as "embeddingDimension",
               embedding_prompt_version as "embeddingPromptVersion",
               chunker_version as "chunkerVersion", content_hash as "contentHash",
               embedding_fingerprint as "embeddingFingerprint",
               embedding::text as "embeddingLiteral", created_at as "createdAt"
        from ai.knowledge_staged_embeddings where chunk_id in (:chunkIds)
        """, nativeQuery = true)
    List<StagedEmbeddingProjection> findRowsByChunkIds(@Param("chunkIds") Collection<UUID> chunkIds);

    @Query(value = "select count(*) from ai.knowledge_staged_embeddings where chunk_id=:chunkId",
            nativeQuery = true)
    long countByChunkId(@Param("chunkId") UUID chunkId);

    static EmbeddingVector fromVectorLiteral(String literal, int dimension) {
        if (literal == null || literal.length() < 2 || literal.charAt(0) != '['
                || literal.charAt(literal.length() - 1) != ']') {
            throw new IllegalArgumentException("Invalid pgvector literal");
        }
        String content = literal.substring(1, literal.length() - 1).trim();
        if (content.isEmpty()) return EmbeddingVector.of(new double[0], dimension);
        String[] encodedValues = content.split(",");
        double[] values = new double[encodedValues.length];
        for (int i = 0; i < encodedValues.length; i++) values[i] = Double.parseDouble(encodedValues[i].trim());
        return EmbeddingVector.of(values, dimension);
    }
}
