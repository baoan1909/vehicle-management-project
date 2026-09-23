package com.ban.vehicle_management.application.ai.port.out;

import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeExtractionResult;

/**
 * Parses an uploaded document into semantic blocks. Implementations are file-type
 * specific (PDFBox, Apache POI, Jsoup, CommonMark) and are never trusted with
 * framework or persistence concerns.
 */
public interface KnowledgeExtractionPort {

    /**
     * @param bytes the full document content
     * @param extension normalized file extension (pdf, docx, html, htm, md, txt)
     * @param mimeType canonical MIME resolved from the extension
     * @throws com.ban.vehicle_management.shared.exception.BadRequestException when the
     *         format is unsupported or the content is unreadable
     */
    KnowledgeExtractionResult extract(byte[] bytes, String extension, String mimeType);
}