package com.ban.vehicle_management.application.ai.service;

import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeChunkDraft;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeChunkingResult;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeDocumentBlock;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeExtractionResult;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeExtractionResult.BlockKind;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeExtractionResult.ExtractedBlock;
import com.ban.vehicle_management.domain.ai.knowledge.policy.KnowledgeChunkingPolicy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Turns extracted semantic blocks into persisted blocks and retrieval chunks. Chunks
 * respect the configured target/max/min/overlap token budgets and always flush on a
 * heading when the buffer already carries meaningful content.
 */
@Component
public class KnowledgeDocumentChunker {

    public KnowledgeChunkingResult chunk(
            UUID documentId,
            int documentVersion,
            String fallbackTitle,
            KnowledgeExtractionResult extraction) {
        List<KnowledgeDocumentBlock> blocks = createBlocks(documentId, documentVersion, extraction);
        List<KnowledgeChunkDraft> chunks = createChunks(documentId, documentVersion, fallbackTitle, blocks);
        return new KnowledgeChunkingResult(blocks, chunks);
    }

    public List<KnowledgeDocumentBlock> createBlocks(
            UUID documentId,
            int documentVersion,
            KnowledgeExtractionResult extraction) {
        List<KnowledgeDocumentBlock> blocks = new ArrayList<>();
        int index = 0;
        for (ExtractedBlock extracted : extraction.blocks()) {
            String text = extracted.text() == null ? "" : extracted.text().strip();
            if (text.isEmpty()) {
                continue;
            }
            blocks.add(KnowledgeDocumentBlock.create(
                    documentId,
                    documentVersion,
                    index++,
                    toBlockKind(extracted.kind()),
                    extracted.headingPath(),
                    extracted.page(),
                    extracted.headingPath(),
                    text,
                    Instant.now()));
        }
        return blocks;
    }

    public List<KnowledgeChunkDraft> createChunks(
            UUID documentId,
            int documentVersion,
            String fallbackTitle,
            List<KnowledgeDocumentBlock> blocks) {
        List<KnowledgeChunkDraft> chunks = new ArrayList<>();
        List<KnowledgeDocumentBlock> buffer = new ArrayList<>();
        long bufferTokens = 0;
        String title = fallbackTitle == null || fallbackTitle.isBlank() ? "Tài liệu" : fallbackTitle;

        for (KnowledgeDocumentBlock block : blocks) {
            long blockTokens = KnowledgeChunkingPolicy.estimateTokens(block.getContent());
            boolean headingBoundary = block.getKind() == KnowledgeDocumentBlock.BlockKind.HEADING;
            boolean overflows = bufferTokens + blockTokens > KnowledgeChunkingPolicy.CHUNK_MAX_TOKENS;
            boolean reachedTarget = bufferTokens >= KnowledgeChunkingPolicy.CHUNK_TARGET_TOKENS;
            if (!buffer.isEmpty() && (overflows || (reachedTarget && (headingBoundary || bufferTokens >= KnowledgeChunkingPolicy.CHUNK_TARGET_TOKENS)))) {
                chunks.add(buildChunk(documentId, documentVersion, title, chunks.size(), buffer));
                buffer = overlapTail(buffer);
                bufferTokens = buffer.stream()
                        .mapToLong(item -> KnowledgeChunkingPolicy.estimateTokens(item.getContent()))
                        .sum();
            }
            buffer.add(block);
            bufferTokens += blockTokens;
        }
        if (!buffer.isEmpty()) {
            chunks.add(buildChunk(documentId, documentVersion, title, chunks.size(), buffer));
        }
        return chunks;
    }

    private List<KnowledgeDocumentBlock> overlapTail(List<KnowledgeDocumentBlock> flushed) {
        List<KnowledgeDocumentBlock> tail = new ArrayList<>();
        long tokens = 0;
        for (int index = flushed.size() - 1; index >= 0; index--) {
            KnowledgeDocumentBlock block = flushed.get(index);
            long blockTokens = KnowledgeChunkingPolicy.estimateTokens(block.getContent());
            if (tokens + blockTokens > KnowledgeChunkingPolicy.CHUNK_OVERLAP_TOKENS) {
                break;
            }
            tail.add(0, block);
            tokens += blockTokens;
        }
        return tail;
    }

    private KnowledgeChunkDraft buildChunk(
            UUID documentId,
            int documentVersion,
            String title,
            int chunkIndex,
            List<KnowledgeDocumentBlock> blocks) {
        KnowledgeDocumentBlock first = blocks.get(0);
        KnowledgeDocumentBlock last = blocks.get(blocks.size() - 1);
        String content = String.join("\n", blocks.stream().map(KnowledgeDocumentBlock::getContent).toList());
        return KnowledgeChunkDraft.of(
                documentId,
                documentVersion,
                title,
                content,
                first.getSourcePage(),
                first.getSourceSection() != null ? first.getSourceSection() : first.getHeadingPath(),
                chunkIndex,
                first.getHeadingPath(),
                first.getBlockIndex(),
                last.getBlockIndex());
    }

    private BlockKind toBlockKind(KnowledgeDocumentBlock.BlockKind kind) {
        return switch (kind) {
            case HEADING -> BlockKind.HEADING;
            case TABLE -> BlockKind.TABLE;
            case LIST -> BlockKind.LIST;
            case PAGE_MARKER -> BlockKind.PAGE_MARKER;
            case PARAGRAPH -> BlockKind.PARAGRAPH;
        };
    }

    private KnowledgeDocumentBlock.BlockKind toBlockKind(BlockKind kind) {
        return switch (kind) {
            case HEADING -> KnowledgeDocumentBlock.BlockKind.HEADING;
            case TABLE -> KnowledgeDocumentBlock.BlockKind.TABLE;
            case LIST -> KnowledgeDocumentBlock.BlockKind.LIST;
            case PAGE_MARKER -> KnowledgeDocumentBlock.BlockKind.PAGE_MARKER;
            case PARAGRAPH -> KnowledgeDocumentBlock.BlockKind.PARAGRAPH;
        };
    }
}
