package com.ban.vehicle_management.application.parking.parkingsession.model.result;

import com.ban.vehicle_management.domain.parking.parkingevent.model.ParkingEvent;
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import java.util.UUID;

public record CheckInResult(
        ParkingSession parkingSession,
        ParkingEvent parkingEvent,
        UUID subscriptionId,
        String customerType,
        String barrierAction
) {
}
