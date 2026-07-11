package com.ban.vehicle_management.application.parking.ocr.usecase;

import com.ban.vehicle_management.application.parking.ocr.model.LicensePlateOcrResult;
import com.ban.vehicle_management.application.parking.ocr.port.in.LicensePlateOcrPortIn;
import com.ban.vehicle_management.application.parking.ocr.port.out.LicensePlateOcrPortOut;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LicensePlateOcrUseCaseImpl implements LicensePlateOcrPortIn {

    private final LicensePlateOcrPortOut licensePlateOcrPortOut;

    public LicensePlateOcrUseCaseImpl(LicensePlateOcrPortOut licensePlateOcrPortOut) {
        this.licensePlateOcrPortOut = licensePlateOcrPortOut;
    }

    @Override
    public LicensePlateOcrResult recognize(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BadRequestException("image must not be empty");
        }
        return licensePlateOcrPortOut.recognize(image);
    }
}
