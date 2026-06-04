package com.ban.vehicle_management.entrypoint.dto.people.customer.request;

import java.util.UUID;

public record UpdateCustomerAdminVehicleRequest(
        UUID customerVehicleId,
        UUID vehicleTypeId,
        String licensePlate,
        String brand,
        String color,
        Boolean isDefault
) {
}
