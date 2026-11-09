package com.ban.vehicle_management.infrastructure.persistence.adapter.catalog;

import com.ban.vehicle_management.application.catalog.availability.port.out.ParkingLotCatalogAvailabilityPortOut;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.TicketTypeRepository;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.catalog.VehicleTypeRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ParkingLotCatalogAvailabilityPersistenceAdapter implements ParkingLotCatalogAvailabilityPortOut {
    private final VehicleTypeRepository vehicleTypeRepository;
    private final TicketTypeRepository ticketTypeRepository;

    public ParkingLotCatalogAvailabilityPersistenceAdapter(
            VehicleTypeRepository vehicleTypeRepository,
            TicketTypeRepository ticketTypeRepository) {
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.ticketTypeRepository = ticketTypeRepository;
    }

    @Override
    public Set<UUID> findExcludedVehicleTypeIds(UUID parkingLotId) {
        return vehicleTypeRepository.findExcludedIdsByParkingLotId(parkingLotId);
    }

    @Override
    public Set<UUID> findExcludedTicketTypeIds(UUID parkingLotId) {
        return ticketTypeRepository.findExcludedIdsByParkingLotId(parkingLotId);
    }

    @Override
    public boolean isVehicleTypeEnabled(UUID parkingLotId, UUID vehicleTypeId) {
        return !vehicleTypeRepository.existsParkingLotExclusion(parkingLotId, vehicleTypeId);
    }

    @Override
    public boolean isTicketTypeEnabled(UUID parkingLotId, UUID ticketTypeId) {
        return !ticketTypeRepository.existsParkingLotExclusion(parkingLotId, ticketTypeId);
    }

    @Override
    public void setVehicleTypeEnabled(UUID organizationId, UUID parkingLotId, UUID vehicleTypeId, boolean enabled) {
        if (enabled) {
            vehicleTypeRepository.deleteParkingLotExclusion(parkingLotId, vehicleTypeId);
        } else {
            vehicleTypeRepository.insertParkingLotExclusion(organizationId, parkingLotId, vehicleTypeId);
        }
    }

    @Override
    public void setTicketTypeEnabled(UUID organizationId, UUID parkingLotId, UUID ticketTypeId, boolean enabled) {
        if (enabled) {
            ticketTypeRepository.deleteParkingLotExclusion(parkingLotId, ticketTypeId);
        } else {
            ticketTypeRepository.insertParkingLotExclusion(organizationId, parkingLotId, ticketTypeId);
        }
    }
}
