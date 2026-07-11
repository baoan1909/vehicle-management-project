package com.ban.vehicle_management.application.parking.parkingevent.port.out;

import com.ban.vehicle_management.domain.parking.parkingevent.model.ParkingEvent;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingEventType;
import java.util.Optional;
import java.util.UUID;

public interface ParkingEventPortOut {

    ParkingEvent save(ParkingEvent parkingEvent);

    Optional<ParkingEvent> findLatestBySessionIdAndEventType(UUID parkingSessionId, ParkingEventType eventType);
}
