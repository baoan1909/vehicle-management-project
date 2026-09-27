package com.ban.vehicle_management.application.catalog.availability.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.catalog.availability.port.out.ParkingLotCatalogAvailabilityPortOut;
import com.ban.vehicle_management.application.catalog.tickettype.port.out.TicketTypePortOut;
import com.ban.vehicle_management.application.catalog.vehicletype.port.out.VehicleTypePortOut;
import com.ban.vehicle_management.application.iam.organization.port.out.OrganizationPortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType;
import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.shared.enumeration.catalog.TicketTypeStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublicParkingLotCatalogUseCaseImplTest {
    private static final UUID ORGANIZATION_ID = UUID.randomUUID();
    private static final UUID LOT_ID = UUID.randomUUID();

    @Mock private VehicleTypePortOut vehicleTypePortOut;
    @Mock private TicketTypePortOut ticketTypePortOut;
    @Mock private OrganizationPortOut organizationPortOut;
    @Mock private ParkingLotPortOut parkingLotPortOut;
    @Mock private ParkingLotCatalogAvailabilityPortOut availabilityPortOut;
    @InjectMocks private PublicParkingLotCatalogUseCaseImpl useCase;

    @Test
    void publicVehicleTypesExcludeOnlyTypesDisabledAtSelectedLot() {
        UUID allowedId = UUID.randomUUID();
        UUID excludedId = UUID.randomUUID();
        when(parkingLotPortOut.findById(LOT_ID)).thenReturn(Optional.of(activeLot()));
        when(availabilityPortOut.findExcludedVehicleTypeIds(LOT_ID)).thenReturn(Set.of(excludedId));
        when(vehicleTypePortOut.findAll(true, Set.of(ORGANIZATION_ID)))
                .thenReturn(List.of(vehicleType(allowedId), vehicleType(excludedId)));

        assertEquals(List.of(allowedId), useCase.getVehicleTypes(LOT_ID).stream()
                .map(VehicleType::getVehicleTypeId).toList());
    }

    @Test
    void publicTicketTypesExcludeOnlyTypesDisabledAtSelectedLot() {
        UUID allowedId = UUID.randomUUID();
        UUID excludedId = UUID.randomUUID();
        when(parkingLotPortOut.findById(LOT_ID)).thenReturn(Optional.of(activeLot()));
        when(availabilityPortOut.findExcludedTicketTypeIds(LOT_ID)).thenReturn(Set.of(excludedId));
        when(ticketTypePortOut.findAll(TicketTypeStatus.ACTIVE, null, Set.of(ORGANIZATION_ID)))
                .thenReturn(List.of(ticketType(allowedId), ticketType(excludedId)));

        assertEquals(List.of(allowedId), useCase.getTicketTypes(LOT_ID, null).stream()
                .map(TicketType::getTicketTypeId).toList());
    }

    private ParkingLot activeLot() {
        ParkingLot lot = new ParkingLot();
        lot.setParkingLotId(LOT_ID);
        lot.setOrganizationId(ORGANIZATION_ID);
        lot.setStatus(ParkingLotStatus.ACTIVE);
        return lot;
    }

    private VehicleType vehicleType(UUID id) {
        VehicleType type = new VehicleType();
        type.setVehicleTypeId(id);
        return type;
    }

    private TicketType ticketType(UUID id) {
        TicketType type = new TicketType();
        type.setTicketTypeId(id);
        return type;
    }
}
