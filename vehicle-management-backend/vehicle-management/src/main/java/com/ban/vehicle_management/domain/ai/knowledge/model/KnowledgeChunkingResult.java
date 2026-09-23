package com.ban.vehicle_management.domain.ai.knowledge.model;

import java.util.List;

public record KnowledgeChunkingResult(
        List<KnowledgeDocumentBlock> blocks,
        List<KnowledgeChunkDraft> chunks
) {
    public boolean isEmpty() {
        return chunks == null || chunks.isEmpty();
    }
}