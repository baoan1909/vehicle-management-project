package com.ban.vehicle_management.entrypoint.dto.people.customer.request;

import java.util.List;
import java.util.UUID;

public record UpdateCustomerAdminVehicleOperationsRequest(
        List<CreateCustomerAdminVehicleRequest> create,
        List<UpdateCustomerAdminVehicleRequest> update,
        List<UUID> inactivate
) {
}
