package com.ban.vehicle_management.entrypoint.dto.catalog.vehicletype.request;

public record CreateVehicleTypeRequest(
        String code,
        String name,
        String description,
        Boolean isActive
) {
}

