package com.ban.vehicle_management.entrypoint.dto.people.customervehicle.request;

import com.ban.vehicle_management.shared.enumeration.people.CustomerVehicleStatus;
import java.util.UUID;

public record CustomerVehicleFilterRequest(
        UUID customerId,
        CustomerVehicleStatus status,
        UUID vehicleTypeId,
        Boolean isDefault,
        String keyword
) {
}

