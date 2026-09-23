package com.ban.vehicle_management.infrastructure.ai.extraction;

import com.ban.vehicle_management.application.ai.port.out.KnowledgeExtractionPort;
import com.ban.vehicle_management.domain.ai.knowledge.model.KnowledgeExtractionResult;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Routes extraction to the extractor registered for the document extension. The
 * extension has already been validated against the content signature by the security
 * validator, so a missing extractor here means the format is not supported.
 */
@Component
public class CompositeDocumentExtractor implements KnowledgeExtractionPort {

    private final List<KnowledgeDocumentExtractor> extractors;

    public CompositeDocumentExtractor(List<KnowledgeDocumentExtractor> extractors) {
        this.extractors = extractors;
    }

    @Override
    public KnowledgeExtractionResult extract(byte[] bytes, String extension, String mimeType) {
        return extractors.stream()
                .filter(extractor -> extractor.supports(extension))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("UNSUPPORTED_FILE_TYPE: không có bộ trích xuất cho " + extension))
                .extract(bytes, extension, mimeType);
    }
}