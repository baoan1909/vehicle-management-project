package com.ban.vehicle_management.entrypoint.dto.ai.knowledge.request;

import java.util.UUID;

public record UploadKnowledgeDocumentRequest(
        UUID sourceId,
        String title,
        String originalFilename,
        String contentType,
        byte[] content,
        String idempotencyKey) {
}
