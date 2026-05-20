package com.ban.vehicle_management.entrypoint.dto.catalog.vehicletype.request;

public record UpdateVehicleTypeRequest(
        String code,
        String name,
        String description,
        Boolean isActive
) {
}

