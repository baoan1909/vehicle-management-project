package com.ban.vehicle_management.domain.ai.knowledge.model;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Result of chunking an extracted document: the ordered, non-overlapping chunks with
 * the block index range and token estimate for the ingest job to persist.
 */
@Getter
@Setter
public class KnowledgeChunkDraft {

    private UUID documentId;
    private Integer documentVersion;
    private String title;
    private String content;
    private String summary;
    private Integer sourcePage;
    private String sourceSection;
    private Integer chunkIndex;
    private String headingPath;
    private Integer tokenCount;
    private String contentHash;
    private Integer startBlockIndex;
    private Integer endBlockIndex;

    public static KnowledgeChunkDraft of(
            UUID documentId,
            int documentVersion,
            String title,
            String content,
            Integer sourcePage,
            String sourceSection,
            int chunkIndex,
            String headingPath,
            int startBlockIndex,
            int endBlockIndex) {
        KnowledgeChunkDraft draft = new KnowledgeChunkDraft();
        draft.documentId = documentId;
        draft.documentVersion = documentVersion;
        draft.title = title;
        draft.content = content;
        draft.summary = content.length() > 280 ? content.substring(0, 280) : content;
        draft.sourcePage = sourcePage;
        draft.sourceSection = sourceSection;
        draft.chunkIndex = chunkIndex;
        draft.headingPath = headingPath;
        draft.tokenCount = (int) Math.max(1, Math.round(content.length() / 4.0));
        draft.contentHash = com.ban.vehicle_management.domain.ai.knowledge.policy.KnowledgeChunkingPolicy.estimateTokens(content) > 0
                ? sha256(content)
                : "";
        draft.startBlockIndex = startBlockIndex;
        draft.endBlockIndex = endBlockIndex;
        return draft;
    }

    private static String sha256(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}