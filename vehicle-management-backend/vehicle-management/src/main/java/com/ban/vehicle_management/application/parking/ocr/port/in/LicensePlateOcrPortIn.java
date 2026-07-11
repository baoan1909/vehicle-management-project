package com.ban.vehicle_management.application.parking.ocr.port.in;

import com.ban.vehicle_management.application.parking.ocr.model.LicensePlateOcrResult;
import org.springframework.web.multipart.MultipartFile;

public interface LicensePlateOcrPortIn {
    LicensePlateOcrResult recognize(MultipartFile image);
}
