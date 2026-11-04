package com.ban.vehicle_management.application.catalog.availability.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;

import com.ban.vehicle_management.application.catalog.authorization.CatalogAccessGuard;
import com.ban.vehicle_management.application.catalog.availability.port.out.ParkingLotCatalogAvailabilityPortOut;
import com.ban.vehicle_management.application.catalog.tickettype.port.out.TicketTypePortOut;
import com.ban.vehicle_management.application.catalog.vehicletype.port.out.VehicleTypePortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.iam.organization.authorization.OrganizationAccessGuard;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType;
import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import org.springframework.security.access.AccessDeniedException;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParkingLotCatalogAvailabilityUseCaseImplTest {
    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final UUID LOT_ID = UUID.randomUUID();
    private static final UUID VEHICLE_TYPE_ID = UUID.randomUUID();
    private static final UUID TICKET_TYPE_ID = UUID.randomUUID();

    @Mock private CurrentAccountPortIn currentAccountPortIn;
    @Mock private CatalogAccessGuard catalogAccessGuard;
    @Mock private OrganizationAccessGuard organizationAccessGuard;
    @Mock private ParkingLotPortOut parkingLotPortOut;
    @Mock private VehicleTypePortOut vehicleTypePortOut;
    @Mock private TicketTypePortOut ticketTypePortOut;
    @Mock private ParkingLotCatalogAvailabilityPortOut availabilityPortOut;
    @InjectMocks private ParkingLotCatalogAvailabilityUseCaseImpl useCase;

    @Test
    void readsAvailabilityOnlyAfterCheckingLotScope() {
        ParkingLot lot = lot();
        when(parkingLotPortOut.findById(LOT_ID)).thenReturn(Optional.of(lot));
        when(availabilityPortOut.findExcludedVehicleTypeIds(LOT_ID)).thenReturn(Set.of(VEHICLE_TYPE_ID));
        when(availabilityPortOut.findExcludedTicketTypeIds(LOT_ID)).thenReturn(Set.of(TICKET_TYPE_ID));

        var result = useCase.getAvailability(LOT_ID);

        assertEquals(Set.of(VEHICLE_TYPE_ID), result.excludedVehicleTypeIds());
        assertEquals(Set.of(), result.drainingVehicleTypeIds());
        assertEquals(Set.of(TICKET_TYPE_ID), result.excludedTicketTypeIds());
        verify(currentAccountPortIn).requirePermission("PARKING_LOT_READ_ALL");
        verify(organizationAccessGuard).ensureCanAccessParkingLot(lot);
        verify(catalogAccessGuard).ensureReadable(ORGANIZATION_ID);
        verify(vehicleTypePortOut).detachVehicleTypeFromZones(LOT_ID, VEHICLE_TYPE_ID);
    }

    @Test
    void cannotConfigureVehicleTypeFromAnotherPartner() {
        when(parkingLotPortOut.findById(LOT_ID)).thenReturn(Optional.of(lot()));
        VehicleType foreignType = new VehicleType();
        foreignType.setOrganizationId(UUID.randomUUID());
        when(vehicleTypePortOut.findById(VEHICLE_TYPE_ID)).thenReturn(Optional.of(foreignType));

        assertThrows(BadRequestException.class,
                () -> useCase.setVehicleTypeEnabled(LOT_ID, VEHICLE_TYPE_ID, false));
        verify(currentAccountPortIn).requirePermission("VEHICLE_TYPE_UPDATE_ALL");
        verify(organizationAccessGuard).ensureCanOperateParkingLot(org.mockito.ArgumentMatchers.any(ParkingLot.class));
    }

    @Test
    void keepsZoneAssignmentsWhilePaidSubscriptionOrOpenSessionStillNeedsService() {
        when(parkingLotPortOut.findById(LOT_ID)).thenReturn(Optional.of(lot()));
        when(availabilityPortOut.findExcludedVehicleTypeIds(LOT_ID)).thenReturn(Set.of(VEHICLE_TYPE_ID));
        when(availabilityPortOut.findExcludedTicketTypeIds(LOT_ID)).thenReturn(Set.of());
        when(vehicleTypePortOut.countServiceObligationSubscriptions(
                org.mockito.ArgumentMatchers.eq(LOT_ID),
                org.mockito.ArgumentMatchers.eq(VEHICLE_TYPE_ID),
                org.mockito.ArgumentMatchers.any(java.time.LocalDate.class)))
                .thenReturn(1L);

        var result = useCase.getAvailability(LOT_ID);

        assertEquals(Set.of(VEHICLE_TYPE_ID), result.drainingVehicleTypeIds());
        verify(vehicleTypePortOut, org.mockito.Mockito.never())
                .detachVehicleTypeFromZones(LOT_ID, VEHICLE_TYPE_ID);
    }

    @Test
    void assignedManagerCannotApplyOneChangeToMultipleParkingLots() {
        UUID anotherLotId = UUID.randomUUID();
        doNothing().when(currentAccountPortIn).requirePermission("VEHICLE_TYPE_UPDATE_ALL");
        doThrow(new AccessDeniedException("partner scope required"))
                .when(currentAccountPortIn).requirePermission(OrganizationAccessGuard.PARKING_SCOPE_PARTNER);

        assertThrows(AccessDeniedException.class,
                () -> useCase.setVehicleTypeEnabled(Set.of(LOT_ID, anotherLotId), VEHICLE_TYPE_ID, false));
    }

    @Test
    void assignedManagerCanStopVehicleServiceAtAssignedParkingLot() {
        ParkingLot assignedLot = lot();
        VehicleType vehicleType = new VehicleType();
        vehicleType.setOrganizationId(ORGANIZATION_ID);
        when(parkingLotPortOut.findById(LOT_ID)).thenReturn(Optional.of(assignedLot));
        when(vehicleTypePortOut.findById(VEHICLE_TYPE_ID)).thenReturn(Optional.of(vehicleType));

        useCase.setVehicleTypeEnabled(LOT_ID, VEHICLE_TYPE_ID, false);

        verify(currentAccountPortIn).requirePermission("VEHICLE_TYPE_UPDATE_ALL");
        verify(organizationAccessGuard).ensureCanOperateParkingLot(assignedLot);
        verify(availabilityPortOut).setVehicleTypeEnabled(
                ORGANIZATION_ID, LOT_ID, VEHICLE_TYPE_ID, false);
    }

    @Test
    void assignedManagerCannotStopVehicleServiceAtUnassignedParkingLot() {
        ParkingLot unassignedLot = lot();
        VehicleType vehicleType = new VehicleType();
        vehicleType.setOrganizationId(ORGANIZATION_ID);
        when(parkingLotPortOut.findById(LOT_ID)).thenReturn(Optional.of(unassignedLot));
        when(vehicleTypePortOut.findById(VEHICLE_TYPE_ID)).thenReturn(Optional.of(vehicleType));
        doThrow(new AccessDeniedException("parking lot is not assigned"))
                .when(organizationAccessGuard).ensureCanOperateParkingLot(unassignedLot);

        assertThrows(AccessDeniedException.class,
                () -> useCase.setVehicleTypeEnabled(LOT_ID, VEHICLE_TYPE_ID, false));

        verify(availabilityPortOut, never()).setVehicleTypeEnabled(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void partnerStoppingVehicleAtEveryLotAlsoDeactivatesSharedPriceRules() {
        UUID anotherLotId = UUID.randomUUID();
        ParkingLot firstLot = lot();
        ParkingLot secondLot = lot();
        secondLot.setParkingLotId(anotherLotId);
        VehicleType vehicleType = new VehicleType();
        vehicleType.setOrganizationId(ORGANIZATION_ID);
        when(currentAccountPortIn.hasPermission(OrganizationAccessGuard.PARKING_SCOPE_PARTNER)).thenReturn(true);
        when(parkingLotPortOut.findById(LOT_ID)).thenReturn(Optional.of(firstLot));
        when(parkingLotPortOut.findById(anotherLotId)).thenReturn(Optional.of(secondLot));
        when(parkingLotPortOut.findAll(null, null, Set.of(ORGANIZATION_ID), null))
                .thenReturn(List.of(firstLot, secondLot));
        when(vehicleTypePortOut.findById(VEHICLE_TYPE_ID)).thenReturn(Optional.of(vehicleType));

        useCase.setVehicleTypeEnabled(Set.of(LOT_ID, anotherLotId), VEHICLE_TYPE_ID, false);

        verify(vehicleTypePortOut).deactivateActivePriceRules(VEHICLE_TYPE_ID);
    }

    @Test
    void disablingTicketTypeUpdatesOnlySelectedLot() {
        when(parkingLotPortOut.findById(LOT_ID)).thenReturn(Optional.of(lot()));
        TicketType ticketType = new TicketType();
        ticketType.setOrganizationId(ORGANIZATION_ID);
        when(ticketTypePortOut.findById(TICKET_TYPE_ID)).thenReturn(Optional.of(ticketType));
        when(availabilityPortOut.findExcludedVehicleTypeIds(LOT_ID)).thenReturn(Set.of());
        when(availabilityPortOut.findExcludedTicketTypeIds(LOT_ID)).thenReturn(Set.of(TICKET_TYPE_ID));

        var result = useCase.setTicketTypeEnabled(LOT_ID, TICKET_TYPE_ID, false);

        assertEquals(Set.of(TICKET_TYPE_ID), result.excludedTicketTypeIds());
        verify(currentAccountPortIn).requirePermission("TICKET_TYPE_UPDATE_ALL");
        verify(availabilityPortOut).setTicketTypeEnabled(ORGANIZATION_ID, LOT_ID, TICKET_TYPE_ID, false);
    }

    private ParkingLot lot() {
        ParkingLot lot = new ParkingLot();
        lot.setParkingLotId(LOT_ID);
        lot.setOrganizationId(ORGANIZATION_ID);
        return lot;
    }
}
