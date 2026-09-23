package com.ban.vehicle_management.entrypoint.dto.ai.knowledge.response;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KnowledgeChunkResponse {
    private UUID chunkId;
    private Integer documentVersion;
    private String title;
    private String content;
    private String summary;
    private Integer sourcePage;
    private String sourceSection;
    private Integer chunkIndex;
    private Integer tokenCount;
    private String headingPath;
}