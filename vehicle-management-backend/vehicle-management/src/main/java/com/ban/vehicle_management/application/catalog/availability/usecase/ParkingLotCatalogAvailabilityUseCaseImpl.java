package com.ban.vehicle_management.application.catalog.availability.usecase;

import com.ban.vehicle_management.application.catalog.authorization.CatalogAccessGuard;
import com.ban.vehicle_management.application.catalog.availability.model.ParkingLotCatalogAvailability;
import com.ban.vehicle_management.application.catalog.availability.port.in.ParkingLotCatalogAvailabilityPortIn;
import com.ban.vehicle_management.application.catalog.availability.port.out.ParkingLotCatalogAvailabilityPortOut;
import com.ban.vehicle_management.application.catalog.tickettype.port.out.TicketTypePortOut;
import com.ban.vehicle_management.application.catalog.vehicletype.port.out.VehicleTypePortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParkingLotCatalogAvailabilityUseCaseImpl implements ParkingLotCatalogAvailabilityPortIn {
    private final CurrentAccountPortIn currentAccountPortIn;
    private final CatalogAccessGuard catalogAccessGuard;
    private final OrganizationAccessGuard organizationAccessGuard;
    private final ParkingLotPortOut parkingLotPortOut;
    private final VehicleTypePortOut vehicleTypePortOut;
    private final TicketTypePortOut ticketTypePortOut;
    private final ParkingLotCatalogAvailabilityPortOut availabilityPortOut;

    public ParkingLotCatalogAvailabilityUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            CatalogAccessGuard catalogAccessGuard,
            OrganizationAccessGuard organizationAccessGuard,
            ParkingLotPortOut parkingLotPortOut,
            VehicleTypePortOut vehicleTypePortOut,
            TicketTypePortOut ticketTypePortOut,
            ParkingLotCatalogAvailabilityPortOut availabilityPortOut
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.catalogAccessGuard = catalogAccessGuard;
        this.organizationAccessGuard = organizationAccessGuard;
        this.parkingLotPortOut = parkingLotPortOut;
        this.vehicleTypePortOut = vehicleTypePortOut;
        this.ticketTypePortOut = ticketTypePortOut;
        this.availabilityPortOut = availabilityPortOut;
    }

    @Override
    @Transactional(readOnly = true)
    public ParkingLotCatalogAvailability getAvailability(UUID parkingLotId) {
        currentAccountPortIn.requirePermission("PARKING_LOT_READ_ALL");
        ParkingLot lot = findReadableLot(parkingLotId);
        return snapshot(lot);
    }

    @Override
    @Transactional
    public ParkingLotCatalogAvailability setVehicleTypeEnabled(UUID parkingLotId, UUID vehicleTypeId, boolean enabled) {
        currentAccountPortIn.requirePermission("VEHICLE_TYPE_UPDATE_ALL");
        ParkingLot lot = findWritableLot(parkingLotId);
        var vehicleType = vehicleTypePortOut.findById(vehicleTypeId)
                .orElseThrow(() -> new NotFoundException("Vehicle type not found"));
        if (!lot.getOrganizationId().equals(vehicleType.getOrganizationId())) {
            throw new BadRequestException("Vehicle type does not belong to the selected parking lot Partner");
        }
        availabilityPortOut.setVehicleTypeEnabled(lot.getOrganizationId(), parkingLotId, vehicleTypeId, enabled);
        return snapshot(lot);
    }

    @Override
    @Transactional
    public ParkingLotCatalogAvailability setTicketTypeEnabled(UUID parkingLotId, UUID ticketTypeId, boolean enabled) {
        currentAccountPortIn.requirePermission("TICKET_TYPE_UPDATE_ALL");
        ParkingLot lot = findWritableLot(parkingLotId);
        var ticketType = ticketTypePortOut.findById(ticketTypeId)
                .orElseThrow(() -> new NotFoundException("Ticket type not found"));
        if (!lot.getOrganizationId().equals(ticketType.getOrganizationId())) {
            throw new BadRequestException("Ticket type does not belong to the selected parking lot Partner");
        }
        availabilityPortOut.setTicketTypeEnabled(lot.getOrganizationId(), parkingLotId, ticketTypeId, enabled);
        return snapshot(lot);
    }

    private ParkingLot findReadableLot(UUID parkingLotId) {
        ParkingLot lot = parkingLotPortOut.findById(parkingLotId)
                .orElseThrow(() -> new NotFoundException("Parking lot not found"));
        organizationAccessGuard.ensureCanAccessParkingLot(lot);
        catalogAccessGuard.ensureReadable(lot.getOrganizationId());
        return lot;
    }

    private ParkingLot findWritableLot(UUID parkingLotId) {
        ParkingLot lot = findReadableLot(parkingLotId);
        catalogAccessGuard.ensureWritable(lot.getOrganizationId());
        return lot;
    }

    private ParkingLotCatalogAvailability snapshot(ParkingLot lot) {
        return new ParkingLotCatalogAvailability(
                lot.getParkingLotId(),
                lot.getOrganizationId(),
                availabilityPortOut.findExcludedVehicleTypeIds(lot.getParkingLotId()),
                availabilityPortOut.findExcludedTicketTypeIds(lot.getParkingLotId())
        );
    }
}
