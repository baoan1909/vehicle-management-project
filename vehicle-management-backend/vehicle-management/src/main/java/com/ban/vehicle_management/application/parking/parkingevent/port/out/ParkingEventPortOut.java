package com.ban.vehicle_management.application.parking.parkingevent.port.out;

import com.ban.vehicle_management.domain.parking.parkingevent.model.ParkingEvent;

public interface ParkingEventPortOut {

    ParkingEvent save(ParkingEvent parkingEvent);
}
