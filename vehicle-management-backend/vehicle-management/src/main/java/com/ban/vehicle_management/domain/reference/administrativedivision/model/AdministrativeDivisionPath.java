package com.ban.vehicle_management.domain.reference.administrativedivision.model;

public record AdministrativeDivisionPath(
        AdministrativeDivision province,
        AdministrativeDivision district,
        AdministrativeDivision ward
) {
}