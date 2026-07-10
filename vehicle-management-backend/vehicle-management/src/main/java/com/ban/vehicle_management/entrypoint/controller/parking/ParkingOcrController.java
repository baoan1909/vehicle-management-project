package com.ban.vehicle_management.entrypoint.controller.parking;

import com.ban.vehicle_management.application.parking.ocr.model.LicensePlateOcrResult;
import com.ban.vehicle_management.application.parking.ocr.port.in.LicensePlateOcrPortIn;
import com.ban.vehicle_management.entrypoint.dto.parking.ocr.response.LicensePlateOcrResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/parking/ocr")
public class ParkingOcrController {

    private final LicensePlateOcrPortIn licensePlateOcrPortIn;

    public ParkingOcrController(LicensePlateOcrPortIn licensePlateOcrPortIn) {
        this.licensePlateOcrPortIn = licensePlateOcrPortIn;
    }

    @PostMapping(value = "/license-plate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SESSION_CHECK_IN_ALL')")
    public ResponseEntity<ApiResponse<LicensePlateOcrResponse>> recognizeLicensePlate(
            @RequestPart("image") MultipartFile image
    ) {
        LicensePlateOcrResult result = licensePlateOcrPortIn.recognize(image);
        return ResponseEntity.ok(ApiResponse.ok(
                "License plate recognized successfully",
                LicensePlateOcrResponse.from(result)
        ));
    }
}
