package com.ban.vehicle_management.infrastructure.ai.extraction;

import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeExtractionResult;

/**
 * File-type specific extractor. Implementations declare which extension they serve and
 * convert the raw bytes into semantic blocks. They never touch persistence or security.
 */
public interface KnowledgeDocumentExtractor {

    boolean supports(String extension);

    KnowledgeExtractionResult extract(byte[] bytes, String extension, String mimeType);
}