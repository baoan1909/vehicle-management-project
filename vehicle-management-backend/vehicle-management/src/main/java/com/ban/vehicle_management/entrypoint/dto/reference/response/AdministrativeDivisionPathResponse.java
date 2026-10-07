package com.ban.vehicle_management.entrypoint.dto.reference.response;

public record AdministrativeDivisionPathResponse(
        AdministrativeDivisionResponse province,
        AdministrativeDivisionResponse district,
        AdministrativeDivisionResponse ward
) {
}