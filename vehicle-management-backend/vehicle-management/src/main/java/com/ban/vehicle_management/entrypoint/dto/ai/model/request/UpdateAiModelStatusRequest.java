package com.ban.vehicle_management.entrypoint.dto.ai.model.request;

import com.ban.vehicle_management.shared.enumeration.ai.AiModelStatus;

public record UpdateAiModelStatusRequest(AiModelStatus status) {
}
