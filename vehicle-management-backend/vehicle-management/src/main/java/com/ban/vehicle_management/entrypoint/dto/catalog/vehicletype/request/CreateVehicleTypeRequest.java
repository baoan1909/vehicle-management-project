package com.ban.vehicle_management.entrypoint.dto.catalog.vehicletype.request;

public record CreateVehicleTypeRequest(
        String code,
        String name,
        String description,
        Boolean isActive,
        java.util.UUID canonicalVehicleTypeId
) {
    public CreateVehicleTypeRequest(String code, String name, String description, Boolean isActive) {
        this(code, name, description, isActive, null);
    }
}

