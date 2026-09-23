package com.ban.vehicle_management.domain.ai.knowledge.model;

import java.util.List;

/**
 * Textual result of parsing an uploaded document. The extractor is provider-neutral:
 * it emits semantic blocks plus the title and a flattened text used for embedding.
 */
public record KnowledgeExtractionResult(
        String title,
        List<ExtractedBlock> blocks,
        int pageCount
) {
    public boolean isEmpty() {
        return blocks == null || blocks.isEmpty() || blocks.stream().allMatch(block -> block.text() == null || block.text().isBlank());
    }

    public String flattenedText() {
        if (blocks == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (ExtractedBlock block : blocks) {
            if (block.text() != null && !block.text().isBlank()) {
                if (builder.length() > 0) {
                    builder.append("\n");
                }
                builder.append(block.text());
            }
        }
        return builder.toString();
    }

    public record ExtractedBlock(
            String text,
            BlockKind kind,
            Integer page,
            String headingPath
    ) {
    }

    public enum BlockKind {
        HEADING,
        PARAGRAPH,
        TABLE,
        LIST,
        PAGE_MARKER
    }
}