package com.ban.vehicle_management.entrypoint.dto.parking.gate.request;

import com.ban.vehicle_management.shared.enumeration.parking.GateStatus;
import java.util.UUID;

public record GateFilterRequest(
        UUID zoneId,
        GateStatus status,
        String keyword
) {
}