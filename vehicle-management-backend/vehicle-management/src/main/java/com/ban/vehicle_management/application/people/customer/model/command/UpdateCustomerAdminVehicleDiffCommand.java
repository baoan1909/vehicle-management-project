package com.ban.vehicle_management.application.people.customer.model.command;

import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import java.util.List;
import java.util.UUID;

public record UpdateCustomerAdminVehicleDiffCommand(
        List<CustomerVehicle> createVehicles,
        List<CustomerVehicle> updateVehicles,
        List<UUID> inactivateVehicleIds
) {
}
