package com.ban.vehicle_management.application.people.customervehicle.model.command;

import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import java.util.List;
import java.util.UUID;

public record CustomerVehicleBatchCommand(
        UUID customerId,
        List<CustomerVehicle> createVehicles,
        List<CustomerVehicle> updateVehicles,
        List<UUID> inactivateVehicleIds
) {
}
