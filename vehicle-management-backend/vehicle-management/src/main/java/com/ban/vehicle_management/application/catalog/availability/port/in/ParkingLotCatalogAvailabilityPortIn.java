package com.ban.vehicle_management.application.catalog.availability.port.in;

import com.ban.vehicle_management.application.catalog.availability.model.ParkingLotCatalogAvailability;
import java.util.UUID;
import java.util.List;
import java.util.Set;

public interface ParkingLotCatalogAvailabilityPortIn {
    ParkingLotCatalogAvailability getAvailability(UUID parkingLotId);

    ParkingLotCatalogAvailability setVehicleTypeEnabled(UUID parkingLotId, UUID vehicleTypeId, boolean enabled);

    List<ParkingLotCatalogAvailability> setVehicleTypeEnabled(
            Set<UUID> parkingLotIds,
            UUID vehicleTypeId,
            boolean enabled
    );

    ParkingLotCatalogAvailability setTicketTypeEnabled(UUID parkingLotId, UUID ticketTypeId, boolean enabled);
}
