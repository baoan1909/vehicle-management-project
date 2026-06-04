package com.ban.vehicle_management.entrypoint.dto.people.customer.request;

import java.util.UUID;

public record CreateCustomerAdminVehicleRequest(
        UUID vehicleTypeId,
        String licensePlate,
        String brand,
        String color,
        Boolean isDefault
) {
}
