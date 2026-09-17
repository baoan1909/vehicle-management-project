package com.ban.vehicle_management.infrastructure.ai.extraction;

import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeExtractionResult;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeExtractionResult.BlockKind;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeExtractionResult.ExtractedBlock;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Plain text extractor. Heading and list markers are inferred from common plain-text
 * conventions so the chunker still gets useful semantic boundaries.
 */
@Component
public class PlainTextDocumentExtractor implements KnowledgeDocumentExtractor {

    @Override
    public boolean supports(String extension) {
        return "txt".equals(extension) || "text".equals(extension);
    }

    @Override
    public KnowledgeExtractionResult extract(byte[] bytes, String extension, String mimeType) {
        String content = new String(bytes, StandardCharsets.UTF_8);
        List<ExtractedBlock> blocks = new ArrayList<>();
        String title = null;
        String headingPath = null;
        for (String rawParagraph : content.split("\\R\\s*\\R")) {
            String paragraph = rawParagraph.strip();
            if (paragraph.isEmpty()) {
                continue;
            }
            String firstLine = paragraph.lines().findFirst().orElse("").strip();
            BlockKind kind = BlockKind.PARAGRAPH;
            if (firstLine.startsWith("#")) {
                kind = BlockKind.HEADING;
                paragraph = firstLine.replaceFirst("^#+\\s*", "");
                headingPath = paragraph;
                if (title == null) {
                    title = paragraph;
                }
            } else if (firstLine.startsWith("- ") || firstLine.startsWith("* ")) {
                kind = BlockKind.LIST;
                paragraph = paragraph.replaceAll("(?m)^[-*]\\s*", "");
            }
            blocks.add(new ExtractedBlock(paragraph, kind, null, headingPath));
        }
        return new KnowledgeExtractionResult(title, blocks, 1);
    }
}