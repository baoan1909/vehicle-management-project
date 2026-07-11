package com.ban.vehicle_management.application.parking.parkingsession.port.in;

import com.ban.vehicle_management.application.parking.parkingsession.model.command.CheckInCommand;
import com.ban.vehicle_management.application.parking.parkingsession.model.command.CheckOutCommand;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.CheckInResult;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.CheckOutPreviewResult;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.CheckOutResult;

public interface ParkingSessionPortIn {

    CheckInResult checkIn(CheckInCommand command);

    CheckOutResult checkOut(CheckOutCommand command);

    CheckOutPreviewResult previewCheckOutByCardUid(String cardUid);
}
