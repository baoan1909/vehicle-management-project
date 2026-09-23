package com.ban.vehicle_management.infrastructure.ai.extraction;

import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeExtractionResult;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeExtractionResult.BlockKind;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeExtractionResult.ExtractedBlock;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class PdfDocumentExtractor implements KnowledgeDocumentExtractor {

    @Override
    public boolean supports(String extension) {
        return "pdf".equals(extension);
    }

    @Override
    public KnowledgeExtractionResult extract(byte[] bytes, String extension, String mimeType) {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            String title = resolveTitle(document);
            PDFTextStripper stripper = new PDFTextStripper();
            List<ExtractedBlock> blocks = new ArrayList<>();
            int pageCount = document.getNumberOfPages();
            for (int page = 1; page <= pageCount; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document);
                if (pageText == null || pageText.isBlank()) {
                    continue;
                }
                for (String paragraph : pageText.split("\\R\\s*\\R")) {
                    String trimmed = paragraph.strip();
                    if (!trimmed.isEmpty()) {
                        blocks.add(new ExtractedBlock(trimmed, BlockKind.PARAGRAPH, page, null));
                    }
                }
            }
            return new KnowledgeExtractionResult(title, blocks, pageCount);
        } catch (IOException exception) {
            throw new com.ban.vehicle_management.shared.exception.BadRequestException(
                    "EXTRACTION_FAILED: không đọc được tệp PDF");
        }
    }

    private String resolveTitle(PDDocument document) {
        PDDocumentInformation information = document.getDocumentInformation();
        if (information != null && information.getTitle() != null && !information.getTitle().isBlank()) {
            return information.getTitle().strip();
        }
        return null;
    }
}