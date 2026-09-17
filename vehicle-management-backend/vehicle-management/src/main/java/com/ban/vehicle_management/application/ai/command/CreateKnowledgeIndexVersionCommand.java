package com.ban.vehicle_management.application.ai.command;

import java.util.UUID;

public record CreateKnowledgeIndexVersionCommand(UUID modelConfigurationId, String versionCode) {
}
