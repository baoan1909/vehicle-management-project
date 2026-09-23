package com.ban.vehicle_management.application.ai.command;

import java.util.UUID;

public record UploadKnowledgeDocumentCommand(
        UUID sourceId,
        String title,
        String originalFilename,
        String contentType,
        byte[] content,
        String idempotencyKey
) {
}