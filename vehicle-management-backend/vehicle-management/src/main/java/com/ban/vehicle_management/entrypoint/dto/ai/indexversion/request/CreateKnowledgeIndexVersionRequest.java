package com.ban.vehicle_management.entrypoint.dto.ai.indexversion.request;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;

public record CreateKnowledgeIndexVersionRequest(
        @NotNull(message = "Vui lòng chọn cấu hình model embedding") UUID modelConfigurationId,
        String versionCode
) {
}
