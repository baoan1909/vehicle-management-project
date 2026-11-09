package com.ban.vehicle_management.application.parking.customerreservation.usecase;

import com.ban.vehicle_management.application.catalog.vehicletype.port.out.VehicleTypePortOut;
import com.ban.vehicle_management.application.parking.customerreservation.model.ParkingPlateLookupResult;
import com.ban.vehicle_management.application.parking.customerreservation.port.in.ParkingPlateLookupPortIn;
import com.ban.vehicle_management.application.parking.customerreservation.port.out.CustomerReservationPortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkinglevel.port.out.ParkingLevelPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.parking.parkingspace.port.out.ParkingSpacePortOut;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import com.ban.vehicle_management.domain.common.licenseplate.LicensePlatePolicy;
import com.ban.vehicle_management.domain.parking.parkinglevel.model.ParkingLevel;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.shared.enumeration.parking.CustomerReservationStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParkingPlateLookupUseCaseImpl implements ParkingPlateLookupPortIn {
    private final ParkingSessionPortOut sessions;
    private final CustomerReservationPortOut reservations;
    private final VehicleTypePortOut vehicleTypes;
    private final ParkingLotPortOut lots;
    private final ZonePortOut zones;
    private final ParkingLevelPortOut levels;
    private final ParkingSpacePortOut spaces;
    private final LicensePlatePolicy platePolicy = new LicensePlatePolicy();

    public ParkingPlateLookupUseCaseImpl(ParkingSessionPortOut sessions,
            CustomerReservationPortOut reservations, VehicleTypePortOut vehicleTypes,
            ParkingLotPortOut lots, ZonePortOut zones, ParkingLevelPortOut levels,
            ParkingSpacePortOut spaces) {
        this.sessions = sessions;
        this.reservations = reservations;
        this.vehicleTypes = vehicleTypes;
        this.lots = lots;
        this.zones = zones;
        this.levels = levels;
        this.spaces = spaces;
    }

    @Override
    @Transactional(readOnly = true)
    public ParkingPlateLookupResult lookup(String licensePlate) {
        String plate = platePolicy.normalizeRequired(licensePlate, "licensePlate");
        Instant now = Instant.now();
        var openSessions = sessions.findOpenByLicensePlateIn(plate).stream()
                .filter(session -> session.getStatus() == ParkingSessionStatus.OPEN)
                .map(session -> {
                    Location location = location(session.getParkingLotId(), session.getZoneId(),
                            session.getParkingSpaceId());
                    String vehicleTypeName = vehicleTypes.findById(session.getVehicleTypeId())
                            .map(VehicleType::getName).orElse(null);
                    return new ParkingPlateLookupResult.Session(session.getParkingLotId(),
                            location.latitude(), location.longitude(), session.getLicensePlateIn(),
                            vehicleTypeName, session.getCheckInTime(), location.lotName(),
                            location.levelName(), location.zoneName(), location.spaceCode());
                }).toList();
        var heldReservations = reservations.findHeldByPlate(plate, now).stream()
                .filter(reservation -> reservation.getStatus() == CustomerReservationStatus.HELD
                        && reservation.getCheckInDeadline() != null
                        && reservation.getCheckInDeadline().isAfter(now))
                .map(reservation -> {
                    Location location = location(reservation.getParkingLotId(), reservation.getZoneId(),
                            reservation.getParkingSpaceId());
                    return new ParkingPlateLookupResult.Reservation(reservation.getParkingLotId(),
                            location.latitude(), location.longitude(), reservation.getLicensePlate(),
                            reservation.getPrepaidAmount(), reservation.getPaidAt(),
                            reservation.getCheckInDeadline(), location.lotName(),
                            location.levelName(), location.zoneName(), location.spaceCode());
                }).toList();
        return new ParkingPlateLookupResult(openSessions, heldReservations);
    }

    private Location location(UUID lotId, UUID zoneId, UUID spaceId) {
        ParkingLot lot = lotId == null ? null : lots.findById(lotId).orElse(null);
        var zone = zoneId == null ? null : zones.findById(zoneId).orElse(null);
        String levelName = zone == null || zone.getParkingLevelId() == null ? null
                : levels.findById(zone.getParkingLevelId()).map(ParkingLevel::getName).orElse(null);
        String spaceCode = spaceId == null ? null
                : spaces.findById(spaceId).map(ParkingSpace::getCode).orElse(null);
        return new Location(lot == null ? null : lot.getName(),
                lot == null ? null : lot.getLatitude(),
                lot == null ? null : lot.getLongitude(),
                levelName, zone == null ? null : zone.getName(), spaceCode);
    }

    private record Location(String lotName, BigDecimal latitude, BigDecimal longitude,
            String levelName, String zoneName, String spaceCode) { }
}
