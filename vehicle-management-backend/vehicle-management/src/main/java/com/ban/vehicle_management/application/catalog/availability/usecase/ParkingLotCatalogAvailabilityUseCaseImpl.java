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
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParkingLotCatalogAvailabilityUseCaseImpl implements ParkingLotCatalogAvailabilityPortIn {
    private static final String VEHICLE_SERVICE_UPDATE_PERMISSION = "VEHICLE_TYPE_UPDATE_ALL";
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
    @Transactional
    public ParkingLotCatalogAvailability getAvailability(UUID parkingLotId) {
        currentAccountPortIn.requirePermission("PARKING_LOT_READ_ALL");
        ParkingLot lot = findReadableLot(parkingLotId);
        return snapshot(lot);
    }

    @Override
    @Transactional
    public ParkingLotCatalogAvailability setVehicleTypeEnabled(UUID parkingLotId, UUID vehicleTypeId, boolean enabled) {
        return setVehicleTypeEnabled(Set.of(parkingLotId), vehicleTypeId, enabled).get(0);
    }

    @Override
    @Transactional
    public List<ParkingLotCatalogAvailability> setVehicleTypeEnabled(
            Set<UUID> parkingLotIds,
            UUID vehicleTypeId,
            boolean enabled
    ) {
        currentAccountPortIn.requirePermission(VEHICLE_SERVICE_UPDATE_PERMISSION);
        if (parkingLotIds == null || parkingLotIds.isEmpty()) {
            throw new BadRequestException("At least one parking lot must be selected");
        }
        if (parkingLotIds.size() > 1) {
            currentAccountPortIn.requirePermission(OrganizationAccessGuard.PARKING_SCOPE_PARTNER);
        }
        var vehicleType = vehicleTypePortOut.findById(vehicleTypeId)
                .orElseThrow(() -> new NotFoundException("Vehicle type not found"));

        List<ParkingLot> lots = parkingLotIds.stream().map(this::findVehicleServiceWritableLot).toList();
        for (ParkingLot lot : lots) {
            if (!lot.getOrganizationId().equals(vehicleType.getOrganizationId())) {
                throw new BadRequestException("Vehicle type does not belong to the selected parking lot Partner");
            }
        }
        for (ParkingLot lot : lots) {
            availabilityPortOut.setVehicleTypeEnabled(
                    lot.getOrganizationId(), lot.getParkingLotId(), vehicleTypeId, enabled);
        }
        if (!enabled && coversEveryPartnerLot(lots)) {
            vehicleTypePortOut.deactivateActivePriceRules(vehicleTypeId);
        }
        return lots.stream().map(this::snapshot).toList();
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

    private ParkingLot findVehicleServiceWritableLot(UUID parkingLotId) {
        ParkingLot lot = findReadableLot(parkingLotId);
        organizationAccessGuard.ensureCanOperateParkingLot(lot);
        return lot;
    }

    private boolean coversEveryPartnerLot(List<ParkingLot> selectedLots) {
        if (!currentAccountPortIn.hasPermission(OrganizationAccessGuard.PARKING_SCOPE_PARTNER)
                || selectedLots.isEmpty()) {
            return false;
        }
        UUID organizationId = selectedLots.get(0).getOrganizationId();
        if (selectedLots.stream().anyMatch(lot -> !organizationId.equals(lot.getOrganizationId()))) {
            return false;
        }
        Set<UUID> allPartnerLotIds = parkingLotPortOut
                .findAll(null, null, Set.of(organizationId), null)
                .stream()
                .map(ParkingLot::getParkingLotId)
                .collect(java.util.stream.Collectors.toSet());
        Set<UUID> selectedLotIds = selectedLots.stream()
                .map(ParkingLot::getParkingLotId)
                .collect(java.util.stream.Collectors.toSet());
        return !allPartnerLotIds.isEmpty() && selectedLotIds.containsAll(allPartnerLotIds);
    }

    private ParkingLotCatalogAvailability snapshot(ParkingLot lot) {
        Set<UUID> excludedVehicleTypeIds = availabilityPortOut
                .findExcludedVehicleTypeIds(lot.getParkingLotId());
        Set<UUID> drainingVehicleTypeIds = new LinkedHashSet<>();
        LocalDate businessDate = LocalDate.now();

        for (UUID vehicleTypeId : excludedVehicleTypeIds) {
            long subscriptionCount = vehicleTypePortOut.countServiceObligationSubscriptions(
                    lot.getParkingLotId(), vehicleTypeId, businessDate);
            long openSessionCount = vehicleTypePortOut.countOpenParkingSessions(
                    lot.getParkingLotId(), vehicleTypeId);
            if (subscriptionCount > 0 || openSessionCount > 0) {
                drainingVehicleTypeIds.add(vehicleTypeId);
            } else {
                vehicleTypePortOut.detachVehicleTypeFromZones(lot.getParkingLotId(), vehicleTypeId);
            }
        }
        return new ParkingLotCatalogAvailability(
                lot.getParkingLotId(),
                lot.getOrganizationId(),
                excludedVehicleTypeIds,
                Set.copyOf(drainingVehicleTypeIds),
                availabilityPortOut.findExcludedTicketTypeIds(lot.getParkingLotId())
        );
    }
}
