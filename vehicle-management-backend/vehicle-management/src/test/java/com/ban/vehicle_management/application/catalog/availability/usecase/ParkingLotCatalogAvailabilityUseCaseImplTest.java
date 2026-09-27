package com.ban.vehicle_management.application.catalog.availability.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
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
        assertEquals(Set.of(TICKET_TYPE_ID), result.excludedTicketTypeIds());
        verify(currentAccountPortIn).requirePermission("PARKING_LOT_READ_ALL");
        verify(organizationAccessGuard).ensureCanAccessParkingLot(lot);
        verify(catalogAccessGuard).ensureReadable(ORGANIZATION_ID);
    }

    @Test
    void cannotConfigureVehicleTypeFromAnotherPartner() {
        when(parkingLotPortOut.findById(LOT_ID)).thenReturn(Optional.of(lot()));
        VehicleType foreignType = new VehicleType();
        foreignType.setOrganizationId(UUID.randomUUID());
        when(vehicleTypePortOut.findById(VEHICLE_TYPE_ID)).thenReturn(Optional.of(foreignType));

        assertThrows(BadRequestException.class,
                () -> useCase.setVehicleTypeEnabled(LOT_ID, VEHICLE_TYPE_ID, false));
        verify(catalogAccessGuard).ensureWritable(ORGANIZATION_ID);
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
