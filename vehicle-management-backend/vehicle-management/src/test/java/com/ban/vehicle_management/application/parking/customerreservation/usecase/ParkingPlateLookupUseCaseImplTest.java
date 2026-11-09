package com.ban.vehicle_management.application.parking.customerreservation.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.catalog.vehicletype.port.out.VehicleTypePortOut;
import com.ban.vehicle_management.application.parking.customerreservation.port.out.CustomerReservationPortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkinglevel.port.out.ParkingLevelPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.parking.parkingspace.port.out.ParkingSpacePortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import com.ban.vehicle_management.domain.parking.customerreservation.model.CustomerReservation;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.parkinglevel.model.ParkingLevel;
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.shared.enumeration.parking.CustomerReservationStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ParkingPlateLookupUseCaseImplTest {
    @Test
    void returnsOpenSessionsBeforeOnlyUnexpiredPaidReservations() {
        var sessions = mock(ParkingSessionPortOut.class);
        var reservations = mock(CustomerReservationPortOut.class);
        var vehicleTypes = mock(VehicleTypePortOut.class);
        var lots = mock(ParkingLotPortOut.class);
        var zones = mock(ZonePortOut.class);
        var levels = mock(ParkingLevelPortOut.class);
        var spaces = mock(ParkingSpacePortOut.class);
        var useCase = new ParkingPlateLookupUseCaseImpl(sessions, reservations,
                vehicleTypes, lots, zones, levels, spaces);
        UUID lotId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        UUID levelId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();
        Instant now = Instant.now();

        ParkingSession session = new ParkingSession();
        session.setStatus(ParkingSessionStatus.OPEN);
        session.setLicensePlateIn("60A-999.99");
        session.setCheckInTime(now.minusSeconds(900));
        session.setParkingLotId(lotId);
        session.setZoneId(zoneId);
        session.setParkingSpaceId(spaceId);
        session.setVehicleTypeId(vehicleTypeId);
        CustomerReservation held = new CustomerReservation();
        held.setStatus(CustomerReservationStatus.HELD);
        held.setLicensePlate("60A99999");
        held.setPaidAt(now.minusSeconds(100));
        held.setCheckInDeadline(now.plusSeconds(3500));
        held.setPrepaidAmount(BigDecimal.valueOf(30_000));
        held.setParkingLotId(lotId);
        held.setZoneId(zoneId);
        held.setParkingSpaceId(spaceId);
        CustomerReservation expired = new CustomerReservation();
        expired.setStatus(CustomerReservationStatus.HELD);
        expired.setCheckInDeadline(now.minusSeconds(1));

        ParkingLot lot = new ParkingLot();
        lot.setName("Bãi xe Nông Lâm");
        lot.setLatitude(BigDecimal.valueOf(10.87));
        lot.setLongitude(BigDecimal.valueOf(106.77));
        Zone zone = new Zone();
        zone.setName("Khu A");
        zone.setParkingLevelId(levelId);
        ParkingLevel level = new ParkingLevel();
        level.setName("Tầng 1");
        ParkingSpace space = new ParkingSpace();
        space.setCode("A-06");
        VehicleType vehicleType = new VehicleType();
        vehicleType.setName("Ô tô");
        when(sessions.findOpenByLicensePlateIn("60A99999")).thenReturn(List.of(session));
        when(reservations.findHeldByPlate(eq("60A99999"), any(Instant.class)))
                .thenReturn(List.of(held, expired));
        when(lots.findById(lotId)).thenReturn(Optional.of(lot));
        when(zones.findById(zoneId)).thenReturn(Optional.of(zone));
        when(levels.findById(levelId)).thenReturn(Optional.of(level));
        when(spaces.findById(spaceId)).thenReturn(Optional.of(space));
        when(vehicleTypes.findById(vehicleTypeId)).thenReturn(Optional.of(vehicleType));

        var result = useCase.lookup("60A-999.99");

        assertEquals(1, result.sessions().size());
        assertEquals("Ô tô", result.sessions().getFirst().vehicleTypeName());
        assertEquals("Bãi xe Nông Lâm", result.sessions().getFirst().parkingLotName());
        assertEquals(lotId, result.sessions().getFirst().parkingLotId());
        assertEquals(BigDecimal.valueOf(10.87), result.sessions().getFirst().latitude());
        assertEquals(BigDecimal.valueOf(106.77), result.reservations().getFirst().longitude());
        assertEquals("Tầng 1", result.sessions().getFirst().parkingLevelName());
        assertEquals("Khu A", result.sessions().getFirst().zoneName());
        assertEquals("A-06", result.sessions().getFirst().parkingSpaceCode());
        assertEquals(1, result.reservations().size());
        assertEquals(BigDecimal.valueOf(30_000), result.reservations().getFirst().prepaidAmount());
        verify(sessions).findOpenByLicensePlateIn("60A99999");
    }
}
