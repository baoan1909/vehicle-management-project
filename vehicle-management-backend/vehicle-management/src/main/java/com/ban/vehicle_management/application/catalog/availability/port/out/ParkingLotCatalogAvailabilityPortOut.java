package com.ban.vehicle_management.application.catalog.availability.port.out;

import java.util.Set;
import java.util.UUID;

public interface ParkingLotCatalogAvailabilityPortOut {
    Set<UUID> findExcludedVehicleTypeIds(UUID parkingLotId);

    Set<UUID> findExcludedTicketTypeIds(UUID parkingLotId);

    boolean isVehicleTypeEnabled(UUID parkingLotId, UUID vehicleTypeId);

    boolean isTicketTypeEnabled(UUID parkingLotId, UUID ticketTypeId);

    void setVehicleTypeEnabled(UUID organizationId, UUID parkingLotId, UUID vehicleTypeId, boolean enabled);

    void setTicketTypeEnabled(UUID organizationId, UUID parkingLotId, UUID ticketTypeId, boolean enabled);
}
