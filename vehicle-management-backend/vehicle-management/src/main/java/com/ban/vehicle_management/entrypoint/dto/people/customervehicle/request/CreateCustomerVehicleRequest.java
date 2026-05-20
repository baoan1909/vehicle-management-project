package com.ban.vehicle_management.entrypoint.dto.people.customervehicle.request;

import java.util.UUID;

public record CreateCustomerVehicleRequest(
        UUID customerId,
        UUID vehicleTypeId,
        String licensePlate,
        String brand,
        String color,
        Boolean isDefault
) {
}
