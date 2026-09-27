package com.ban.vehicle_management.entrypoint.dto.ai.knowledge.response;

import java.time.Instant;

import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeDocumentBlock.BlockKind;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KnowledgeDocumentBlockResponse {
    private UUID blockId;
    private int blockIndex;
    private BlockKind kind;
    private String headingPath;
    private Integer sourcePage;
    private String sourceSection;
    private String content;
    private Instant createdAt;
}
