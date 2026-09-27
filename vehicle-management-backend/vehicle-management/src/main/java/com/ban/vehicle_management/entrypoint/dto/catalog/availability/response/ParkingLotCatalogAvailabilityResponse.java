package com.ban.vehicle_management.entrypoint.dto.catalog.availability.response;

import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParkingLotCatalogAvailabilityResponse {
    private UUID parkingLotId;
    private UUID organizationId;
    private Set<UUID> excludedVehicleTypeIds;
    private Set<UUID> excludedTicketTypeIds;
}
