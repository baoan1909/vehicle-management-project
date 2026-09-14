package com.ban.vehicle_management.domain.ai.model;

public record AiFunctionCall(
        String name,
        String argumentsJson,
        boolean malformed,
        String failureCode
) {
}
