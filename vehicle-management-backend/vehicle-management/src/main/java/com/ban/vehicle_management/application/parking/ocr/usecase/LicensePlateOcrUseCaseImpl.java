package com.ban.vehicle_management.application.parking.ocr.usecase;

import com.ban.vehicle_management.application.parking.ocr.model.LicensePlateOcrCommand;
import com.ban.vehicle_management.application.parking.ocr.model.LicensePlateOcrResult;
import com.ban.vehicle_management.application.parking.ocr.port.in.LicensePlateOcrPortIn;
import com.ban.vehicle_management.application.parking.ocr.port.out.LicensePlateOcrPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.authorization.ParkingSessionAccessGuard;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LicensePlateOcrUseCaseImpl implements LicensePlateOcrPortIn {

    private final LicensePlateOcrPortOut licensePlateOcrPortOut;
    private final ParkingSessionAccessGuard parkingSessionAccessGuard;

    public LicensePlateOcrUseCaseImpl(
            LicensePlateOcrPortOut licensePlateOcrPortOut,
            ParkingSessionAccessGuard parkingSessionAccessGuard
    ) {
        this.licensePlateOcrPortOut = licensePlateOcrPortOut;
        this.parkingSessionAccessGuard = parkingSessionAccessGuard;
    }

    @Override
    public LicensePlateOcrResult recognize(LicensePlateOcrCommand command) {
        parkingSessionAccessGuard.ensureCanUseOcr();
        if (command == null) {
            throw new BadRequestException("OCR command must not be null");
        }
        MultipartFile image = command.image();
        if (image == null || image.isEmpty()) {
            throw new BadRequestException("image must not be empty");
        }

        return licensePlateOcrPortOut.recognize(image);
    }
}
