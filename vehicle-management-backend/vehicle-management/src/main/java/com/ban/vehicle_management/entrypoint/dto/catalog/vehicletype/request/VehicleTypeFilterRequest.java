package com.ban.vehicle_management.entrypoint.dto.catalog.vehicletype.request;

import com.ban.vehicle_management.shared.enumeration.catalog.CatalogLifecycleStatus;

public record VehicleTypeFilterRequest(
        Boolean isActive,
        CatalogLifecycleStatus lifecycleStatus
) {
    public VehicleTypeFilterRequest(Boolean isActive) {
        this(isActive, null);
    }
}
