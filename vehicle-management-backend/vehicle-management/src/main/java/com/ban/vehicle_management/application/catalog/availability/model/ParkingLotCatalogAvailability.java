package com.ban.vehicle_management.application.catalog.availability.model;

import java.util.Set;
import java.util.UUID;

public record ParkingLotCatalogAvailability(
        UUID parkingLotId,
        UUID organizationId,
        Set<UUID> excludedVehicleTypeIds,
        Set<UUID> excludedTicketTypeIds
) {
}
