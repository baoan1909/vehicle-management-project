package com.ban.vehicle_management.application.ai.query;

import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeAccessScope;
import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeSourceStatus;
import java.util.UUID;

public record KnowledgeSourceQuery(
        KnowledgeSourceStatus status,
        KnowledgeAccessScope accessScope,
        UUID tenantId,
        String keyword) {
}
