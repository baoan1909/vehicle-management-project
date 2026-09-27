package com.ban.vehicle_management.application.catalog.availability.port.in;

import com.ban.vehicle_management.application.catalog.availability.model.ParkingLotCatalogAvailability;
import java.util.UUID;

public interface ParkingLotCatalogAvailabilityPortIn {
    ParkingLotCatalogAvailability getAvailability(UUID parkingLotId);

    ParkingLotCatalogAvailability setVehicleTypeEnabled(UUID parkingLotId, UUID vehicleTypeId, boolean enabled);

    ParkingLotCatalogAvailability setTicketTypeEnabled(UUID parkingLotId, UUID ticketTypeId, boolean enabled);
}
