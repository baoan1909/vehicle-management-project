package com.ban.vehicle_management.application.ai.query;

import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeAccessScope;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeDocumentStatus;
import java.time.Instant;
import java.util.UUID;

public record KnowledgeDocumentQuery(
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
