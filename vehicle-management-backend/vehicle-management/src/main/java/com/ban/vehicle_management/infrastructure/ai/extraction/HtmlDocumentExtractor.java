package com.ban.vehicle_management.infrastructure.ai.extraction;

import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeExtractionResult;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeExtractionResult.BlockKind;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeExtractionResult.ExtractedBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

/**
 * Sanitizes and parses HTML with Jsoup. Scripts, styles and comments are ignored; only
 * visible heading/paragraph/list/table text is emitted.
 */
@Component
public class HtmlDocumentExtractor implements KnowledgeDocumentExtractor {

    private static final Set<String> HEADINGS = Set.of("h1", "h2", "h3", "h4", "h5", "h6");

    @Override
    public boolean supports(String extension) {
        return "html".equals(extension) || "htm".equals(extension);
    }

    @Override
    public KnowledgeExtractionResult extract(byte[] bytes, String extension, String mimeType) {
        Document document = Jsoup.parse(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
        document.select("script, style, noscript, template").remove();
        String title = document.title() == null || document.title().isBlank() ? null : document.title().strip();
        Element body = document.body();
        if (body == null) {
            return new KnowledgeExtractionResult(title, List.of(), 1);
        }
        List<ExtractedBlock> blocks = new ArrayList<>();
        String headingPath = null;
        for (Element element : body.select("h1, h2, h3, h4, h5, h6, p, li, tr")) {
            String tag = element.tagName().toLowerCase(java.util.Locale.ROOT);
            String text = element.text();
            if (text == null || text.isBlank()) {
                continue;
            }
            if (HEADINGS.contains(tag)) {
                headingPath = text.strip();
                if (title == null) {
                    title = headingPath;
                }
                blocks.add(new ExtractedBlock(text.strip(), BlockKind.HEADING, null, headingPath));
            } else if ("li".equals(tag)) {
                blocks.add(new ExtractedBlock(text.strip(), BlockKind.LIST, null, headingPath));
            } else if ("tr".equals(tag)) {
                String rowText = flattenRow(element);
                if (!rowText.isBlank()) {
                    blocks.add(new ExtractedBlock(rowText, BlockKind.TABLE, null, headingPath));
                }
            } else {
                blocks.add(new ExtractedBlock(text.strip(), BlockKind.PARAGRAPH, null, headingPath));
            }
        }
        return new KnowledgeExtractionResult(title, blocks, 1);
    }

    private String flattenRow(Element row) {
        Elements cells = row.select("td, th");
        List<String> values = new ArrayList<>();
        for (Element cell : cells) {
            values.add(cell.text().strip());
        }
        return String.join(" | ", values);
    }
}