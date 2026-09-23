package com.ban.vehicle_management.infrastructure.ai.extraction;

import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeExtractionResult;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeExtractionResult.BlockKind;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeExtractionResult.ExtractedBlock;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

@Component
public class DocxDocumentExtractor implements KnowledgeDocumentExtractor {

    @Override
    public boolean supports(String extension) {
        return "docx".equals(extension);
    }

    @Override
    public KnowledgeExtractionResult extract(byte[] bytes, String extension, String mimeType) {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            List<ExtractedBlock> blocks = new ArrayList<>();
            String title = null;
            String headingPath = null;
            for (IBodyElement element : document.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph) {
                    String text = paragraph.getText();
                    if (text == null || text.isBlank()) {
                        continue;
                    }
                    String style = paragraph.getStyle();
                    String normalizedStyle = style == null ? "" : style.toLowerCase(Locale.ROOT);
                    if (normalizedStyle.contains("heading") || normalizedStyle.contains("title")) {
                        headingPath = text.strip();
                        if (title == null) {
                            title = headingPath;
                        }
                        blocks.add(new ExtractedBlock(text.strip(), BlockKind.HEADING, null, headingPath));
                    } else if (paragraph.getNumID() != null) {
                        blocks.add(new ExtractedBlock(stripBullet(text), BlockKind.LIST, null, headingPath));
                    } else {
                        blocks.add(new ExtractedBlock(text.strip(), BlockKind.PARAGRAPH, null, headingPath));
                    }
                } else if (element instanceof XWPFTable table) {
                    String tableText = flattenTable(table);
                    if (!tableText.isBlank()) {
                        blocks.add(new ExtractedBlock(tableText, BlockKind.TABLE, null, headingPath));
                    }
                }
            }
            return new KnowledgeExtractionResult(title, blocks, 1);
        } catch (Exception exception) {
            throw new BadRequestException("EXTRACTION_FAILED: không đọc được tệp DOCX");
        }
    }

    private String flattenTable(XWPFTable table) {
        StringBuilder builder = new StringBuilder();
        for (XWPFTableRow row : table.getRows()) {
            List<String> cells = new ArrayList<>();
            for (XWPFTableCell cell : row.getTableCells()) {
                cells.add(cell.getText() == null ? "" : cell.getText().strip());
            }
            builder.append(String.join(" | ", cells)).append('\n');
        }
        return builder.toString().strip();
    }

    private String stripBullet(String text) {
        return text.strip().replaceFirst("^[\\u2022\\u2023\\u25AA\\u25CF\\-*]\\s*", "");
    }
}