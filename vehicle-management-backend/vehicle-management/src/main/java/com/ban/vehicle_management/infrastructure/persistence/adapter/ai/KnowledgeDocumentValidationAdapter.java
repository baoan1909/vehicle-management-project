package com.ban.vehicle_management.infrastructure.persistence.adapter.ai;

import com.ban.vehicle_management.application.ai.port.out.KnowledgeDocumentValidationPortOut;
import com.ban.vehicle_management.infrastructure.ai.file.DocumentSecurityValidator;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeDocumentValidationAdapter implements KnowledgeDocumentValidationPortOut {

    private final DocumentSecurityValidator validator;

    public KnowledgeDocumentValidationAdapter(DocumentSecurityValidator validator) {
        this.validator = validator;
    }

    @Override
    public void validate(byte[] content, String extension, long maxFileSizeBytes) {
        validator.validate(content, extension, maxFileSizeBytes);
    }
}