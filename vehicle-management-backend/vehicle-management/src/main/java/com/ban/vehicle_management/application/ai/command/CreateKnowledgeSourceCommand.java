package com.ban.vehicle_management.application.ai.command;

import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeAccessScope;

public record CreateKnowledgeSourceCommand(
        String title,
        String description,
        KnowledgeAccessScope accessScope
) {
}