package com.ban.vehicle_management.application.parking.ocr.port.in;

import com.ban.vehicle_management.application.parking.ocr.model.LicensePlateOcrCommand;
import com.ban.vehicle_management.application.parking.ocr.model.LicensePlateOcrResult;

public interface LicensePlateOcrPortIn {
    LicensePlateOcrResult recognize(LicensePlateOcrCommand command);
}