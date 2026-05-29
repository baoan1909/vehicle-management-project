package com.ban.vehicle_management.entrypoint.dto.parking.gate.request;

public record UpdateGateRequest(
        String code,
        String name
) {
}