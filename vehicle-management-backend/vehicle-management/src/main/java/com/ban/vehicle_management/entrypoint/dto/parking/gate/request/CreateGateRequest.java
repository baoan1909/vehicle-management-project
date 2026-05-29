package com.ban.vehicle_management.entrypoint.dto.parking.gate.request;

import java.util.UUID;

public record CreateGateRequest(
        UUID zoneId,
        String code,
        String name
) {
}