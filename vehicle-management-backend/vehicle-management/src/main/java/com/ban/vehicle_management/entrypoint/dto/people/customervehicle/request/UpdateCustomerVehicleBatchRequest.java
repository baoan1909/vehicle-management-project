package com.ban.vehicle_management.entrypoint.dto.people.customervehicle.request;

import java.util.UUID;

public record UpdateCustomerVehicleBatchRequest(
        UUID customerVehicleId,
        UUID vehicleTypeId,
        String licensePlate,
        String brand,
        String color,
        Boolean isDefault
) {
}
