package com.ban.vehicle_management.domain.ai.knowledge.model;

import com.ban.vehicle_management.domain.ai.model.EmbeddingVector;
import com.ban.vehicle_management.shared.enumeration.ai.AiProvider;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Precomputed embedding produced while the document waits for review. The index build
 * reuses staged vectors whose fingerprint matches the eligible chunk so documents are
 * never re-embedded after approval. tenantId/documentId are resolved through the chunk.
 */
@Getter
@Setter
public class KnowledgeStagedEmbedding {

    private UUID stagedEmbeddingId;
    private UUID chunkId;
    private AiProvider provider;
    private UUID modelConfigurationId;
    private String modelId;
    private int embeddingDimension;
    private String embeddingPromptVersion;
    private String chunkerVersion;
    private String contentHash;
    private String embeddingFingerprint;
    private EmbeddingVector embedding;
    private Instant createdAt;

    public static KnowledgeStagedEmbedding of(
            UUID chunkId,
            AiProvider provider,
            UUID modelConfigurationId,
            String modelId,
            int embeddingDimension,
            String embeddingPromptVersion,
            String chunkerVersion,
            String contentHash,
            EmbeddingVector embedding,
            Instant now) {
        KnowledgeStagedEmbedding staged = new KnowledgeStagedEmbedding();
        staged.stagedEmbeddingId = UUID.randomUUID();
        staged.chunkId = chunkId;
        staged.provider = provider;
        staged.modelConfigurationId = modelConfigurationId;
        staged.modelId = modelId;
        staged.embeddingDimension = embeddingDimension;
        staged.embeddingPromptVersion = embeddingPromptVersion;
        staged.chunkerVersion = chunkerVersion;
        staged.contentHash = contentHash;
        staged.embeddingFingerprint = fingerprint(contentHash, modelId, embeddingPromptVersion, chunkerVersion);
        staged.embedding = embedding;
        staged.createdAt = now;
        return staged;
    }

    public static String fingerprint(String contentHash, String modelId, String embeddingPromptVersion, String chunkerVersion) {
        return sha256Hex(contentHash + "|" + modelId + "|" + embeddingPromptVersion + "|" + chunkerVersion);
    }

    public static String sha256Hex(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(bytes);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}