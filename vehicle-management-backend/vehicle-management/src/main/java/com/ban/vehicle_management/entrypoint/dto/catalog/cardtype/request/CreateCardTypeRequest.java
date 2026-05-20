package com.ban.vehicle_management.entrypoint.dto.catalog.cardtype.request;

public record CreateCardTypeRequest(
        String code,
        String name,
        String description,
        Boolean isReturnRequired,
        Boolean isActive
) {
}

