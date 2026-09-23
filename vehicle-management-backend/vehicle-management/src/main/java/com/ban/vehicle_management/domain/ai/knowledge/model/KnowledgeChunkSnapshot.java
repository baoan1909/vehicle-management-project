package com.ban.vehicle_management.domain.ai.knowledge.model;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * Read-side projection of a persisted chunk used by the document detail API.
 */
@Getter
@Setter
public class KnowledgeChunkSnapshot {

    private UUID chunkId;
    private UUID documentId;
    private Integer documentVersion;
    private String title;
    private String content;
    private String summary;
    private Integer sourcePage;
    private String sourceSection;
    private Integer chunkIndex;
    private Integer tokenCount;
    private String headingPath;

    public static KnowledgeChunkSnapshot of(
            UUID chunkId,
            UUID documentId,
            Integer documentVersion,
            String title,
            String content,
            String summary,
            Integer sourcePage,
            String sourceSection,
            Integer chunkIndex,
            Integer tokenCount,
            String headingPath) {
        KnowledgeChunkSnapshot snapshot = new KnowledgeChunkSnapshot();
        snapshot.chunkId = chunkId;
        snapshot.documentId = documentId;
        snapshot.documentVersion = documentVersion;
        snapshot.title = title;
        snapshot.content = content;
        snapshot.summary = summary;
        snapshot.sourcePage = sourcePage;
        snapshot.sourceSection = sourceSection;
        snapshot.chunkIndex = chunkIndex;
        snapshot.tokenCount = tokenCount;
        snapshot.headingPath = headingPath;
        return snapshot;
    }
}