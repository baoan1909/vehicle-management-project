package com.ban.vehicle_management.application.parking.parkingsession.port.out;

import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParkingSessionPortOut {

    ParkingSession save(ParkingSession parkingSession);

    Optional<ParkingSession> findOpenByCardId(UUID cardId);

    boolean existsOpenByCardId(UUID cardId);

    long countOpenByZoneId(UUID zoneId);

    List<ParkingSession> findOpenByLicensePlateIn(String licensePlateIn);

    Optional<ParkingSession> findById(UUID parkingSessionId);
}
