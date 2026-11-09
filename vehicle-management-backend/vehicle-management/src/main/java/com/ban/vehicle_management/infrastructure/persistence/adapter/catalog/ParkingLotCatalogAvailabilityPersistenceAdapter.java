package com.ban.vehicle_management.infrastructure.persistence.adapter.catalog;

import com.ban.vehicle_management.application.catalog.availability.port.out.ParkingLotCatalogAvailabilityPortOut;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.ParkingLotCatalogAvailabilityRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ParkingLotCatalogAvailabilityPersistenceAdapter implements ParkingLotCatalogAvailabilityPortOut {
    private final ParkingLotCatalogAvailabilityRepository repository;

    public ParkingLotCatalogAvailabilityPersistenceAdapter(ParkingLotCatalogAvailabilityRepository repository) {
        this.repository = repository;
    }

    @Override
    public Set<UUID> findExcludedVehicleTypeIds(UUID parkingLotId) {
        return repository.findExcludedVehicleTypeIds(parkingLotId);
    }

    @Override
    public Set<UUID> findExcludedTicketTypeIds(UUID parkingLotId) {
        return repository.findExcludedTicketTypeIds(parkingLotId);
    }

    @Override
    public boolean isVehicleTypeEnabled(UUID parkingLotId, UUID vehicleTypeId) {
        return !repository.existsVehicleTypeExclusion(parkingLotId, vehicleTypeId);
    }

    @Override
    public boolean isTicketTypeEnabled(UUID parkingLotId, UUID ticketTypeId) {
        return !repository.existsTicketTypeExclusion(parkingLotId, ticketTypeId);
    }

    @Override
    public void setVehicleTypeEnabled(UUID organizationId, UUID parkingLotId, UUID vehicleTypeId, boolean enabled) {
        if (enabled) {
            repository.deleteVehicleTypeExclusion(parkingLotId, vehicleTypeId);
        } else {
            repository.insertVehicleTypeExclusion(organizationId, parkingLotId, vehicleTypeId);
        }
    }

    @Override
    public void setTicketTypeEnabled(UUID organizationId, UUID parkingLotId, UUID ticketTypeId, boolean enabled) {
        if (enabled) {
            repository.deleteTicketTypeExclusion(parkingLotId, ticketTypeId);
        } else {
            repository.insertTicketTypeExclusion(organizationId, parkingLotId, ticketTypeId);
        }
    }
}
