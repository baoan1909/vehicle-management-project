package com.ban.vehicle_management.entrypoint.dto.people.customervehicle.request;

import java.util.List;
import java.util.UUID;

public record CustomerVehicleBatchRequest(
        UUID customerId,
        List<CreateCustomerVehicleRequest> create,
        List<UpdateCustomerVehicleBatchRequest> update,
        List<UUID> inactivate
) {
}
