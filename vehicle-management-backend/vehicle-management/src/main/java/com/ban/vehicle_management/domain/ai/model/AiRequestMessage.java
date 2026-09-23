package com.ban.vehicle_management.domain.ai.model;

public record AiRequestMessage(
        String role,
        String content
) {
}
