package com.ban.vehicle_management.domain.ai.model;

import com.ban.vehicle_management.shared.enumeration.ai.AiRuntimeMode;
import java.time.Instant;
import java.util.UUID;

public record AiRuntimeState(AiRuntimeMode mode, AiRuntimeMode targetMode, long epoch,
        String status, UUID changedBy, Instant changedAt) {
    public boolean switching() { return "SWITCHING".equals(status); }
    public boolean ready() { return "READY".equals(status); }
}
