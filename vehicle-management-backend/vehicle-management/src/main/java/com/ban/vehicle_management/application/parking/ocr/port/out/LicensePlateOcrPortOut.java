package com.ban.vehicle_management.application.parking.ocr.port.out;

import com.ban.vehicle_management.application.parking.ocr.model.LicensePlateOcrResult;
import org.springframework.web.multipart.MultipartFile;

public interface LicensePlateOcrPortOut {
    LicensePlateOcrResult recognize(MultipartFile image);
}
