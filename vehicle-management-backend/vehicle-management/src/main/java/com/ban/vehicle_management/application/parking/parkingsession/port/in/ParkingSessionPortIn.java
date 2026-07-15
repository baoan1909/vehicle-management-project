package com.ban.vehicle_management.application.parking.parkingsession.port.in;

import com.ban.vehicle_management.application.parking.parkingsession.model.command.CheckInCommand;
import com.ban.vehicle_management.application.parking.parkingsession.model.command.CheckOutCommand;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.CheckInResult;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.CheckOutPreviewResult;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.CheckOutResult;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.ParkingSessionManagementResult;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ParkingSessionPortIn {

    List<ParkingSessionManagementResult> getSessions(
            ParkingSessionStatus status,
            UUID vehicleTypeId,
            UUID zoneId,
            LocalDate fromDate,
            LocalDate toDate,
            String keyword
    );

    List<ParkingSessionManagementResult> getOwnSessions(
            ParkingSessionStatus status,
            UUID vehicleTypeId,
            UUID zoneId,
            LocalDate fromDate,
            LocalDate toDate,
            String keyword
    );

    CheckInResult checkIn(CheckInCommand command);

    CheckOutResult checkOut(CheckOutCommand command);

    CheckOutPreviewResult previewCheckOutByCardUid(String cardUid);
}
