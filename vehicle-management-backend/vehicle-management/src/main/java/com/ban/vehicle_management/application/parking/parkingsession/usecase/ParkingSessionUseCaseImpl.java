package com.ban.vehicle_management.application.parking.parkingsession.usecase;

import com.ban.vehicle_management.application.parking.parkingsession.model.command.CheckInCommand;
import com.ban.vehicle_management.application.parking.parkingsession.model.command.CheckOutCommand;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.CheckInResult;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.CheckOutPreviewResult;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.CheckOutResult;
import com.ban.vehicle_management.application.parking.parkingsession.port.in.ParkingSessionPortIn;
import org.springframework.stereotype.Service;

@Service
public class ParkingSessionUseCaseImpl implements ParkingSessionPortIn {

    private final ParkingCheckInUseCaseImpl parkingCheckInUseCase;
    private final ParkingCheckOutUseCaseImpl parkingCheckOutUseCase;

    public ParkingSessionUseCaseImpl(
            ParkingCheckInUseCaseImpl parkingCheckInUseCase,
            ParkingCheckOutUseCaseImpl parkingCheckOutUseCase
    ) {
        this.parkingCheckInUseCase = parkingCheckInUseCase;
        this.parkingCheckOutUseCase = parkingCheckOutUseCase;
    }

    @Override
    public CheckInResult checkIn(CheckInCommand command) {
        return parkingCheckInUseCase.checkIn(command);
    }

    @Override
    public CheckOutResult checkOut(CheckOutCommand command) {
        return parkingCheckOutUseCase.checkOut(command);
    }

    @Override
    public CheckOutPreviewResult previewCheckOutByCardUid(String cardUid) {
        return parkingCheckOutUseCase.previewCheckOutByCardUid(cardUid);
    }
}
