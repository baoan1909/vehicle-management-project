package com.ban.vehicle_management.application.parking.parkingsession.port.in;

import com.ban.vehicle_management.application.parking.parkingsession.model.command.CheckInCommand;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.CheckInResult;

public interface ParkingSessionPortIn {

    CheckInResult checkIn(CheckInCommand command);
}
