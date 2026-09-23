package com.ban.vehicle_management.application.ai.command;

import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeAccessScope;
import java.util.UUID;

public record UpdateKnowledgeSourceCommand(
        String title,
        String description,
        KnowledgeAccessScope accessScope,
        UUID tenantId
) {
}
