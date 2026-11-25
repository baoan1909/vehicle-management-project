package com.ban.vehicle_management.entrypoint.dto.ai.model.response;

import com.ban.vehicle_management.shared.enumeration.ai.AiRuntimeMode;
import java.time.Instant;
import java.util.UUID;

public record AiRuntimeModeResponse(AiRuntimeMode mode, AiRuntimeMode targetMode, long epoch,
        String status, UUID changedBy, Instant changedAt) { }
