package com.ban.vehicle_management.entrypoint.dto.ai.knowledge.request;

import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeAccessScope;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeDocumentStatus;
import java.time.Instant;
import java.util.UUID;

public record KnowledgeDocumentFilterRequest(
        UUID sourceId,
        UUID documentKey,
        KnowledgeDocumentStatus status,
        KnowledgeAccessScope accessScope,
        UUID tenantId,
        String fileExtension,
        Instant createdFrom,
        Instant createdTo,
        String keyword) {
}
