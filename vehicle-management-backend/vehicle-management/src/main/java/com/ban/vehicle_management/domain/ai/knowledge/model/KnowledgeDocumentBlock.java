package com.ban.vehicle_management.domain.ai.knowledge.model;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * A semantically meaningful text segment extracted from a document (heading, paragraph,
 * table, list or page marker). The chunker groups consecutive blocks into chunks and
 * records the block index range on the resulting chunk.
 */
@Getter
@Setter
public class KnowledgeDocumentBlock {

    public enum BlockKind {
        HEADING,
        PARAGRAPH,
        TABLE,
        LIST,
        PAGE_MARKER
    }

    private UUID blockId;
    private UUID documentId;
    private int docVersion;
    private int blockIndex;
    private BlockKind kind;
    private String headingPath;
    private Integer sourcePage;
    private String sourceSection;
    private String content;
    private String hash;
    private Instant createdAt;

    public static KnowledgeDocumentBlock create(
            UUID documentId,
            int docVersion,
            int blockIndex,
            BlockKind kind,
            String headingPath,
            Integer sourcePage,
            String sourceSection,
            String content,
            Instant now) {
        KnowledgeDocumentBlock block = new KnowledgeDocumentBlock();
        block.blockId = UUID.randomUUID();
        block.documentId = documentId;
        block.docVersion = docVersion;
        block.blockIndex = blockIndex;
        block.kind = kind;
        block.headingPath = headingPath;
        block.sourcePage = sourcePage;
        block.sourceSection = sourceSection;
        block.content = content;
        block.createdAt = now;
        return block;
    }
}