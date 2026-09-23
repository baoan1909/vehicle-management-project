package com.ban.vehicle_management.entrypoint.dto.ai.knowledge.request;

import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeAccessScope;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeSourceStatus;
import java.util.UUID;

public record KnowledgeSourceFilterRequest(
        KnowledgeSourceStatus status,
        KnowledgeAccessScope accessScope,
        UUID tenantId,
        String keyword) {
}
