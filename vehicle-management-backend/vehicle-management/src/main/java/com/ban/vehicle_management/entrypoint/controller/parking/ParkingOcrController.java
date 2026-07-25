package com.ban.vehicle_management.entrypoint.controller.parking;

import com.ban.vehicle_management.application.parking.ocr.model.LicensePlateOcrCommand;
import com.ban.vehicle_management.application.parking.ocr.model.LicensePlateOcrResult;
import com.ban.vehicle_management.application.parking.ocr.port.in.LicensePlateOcrPortIn;
import com.ban.vehicle_management.entrypoint.dto.parking.ocr.response.LicensePlateOcrResponse;
import com.ban.vehicle_management.shared.enumeration.parking.LaneDirection;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public ResponseEntity<ApiResponse<LicensePlateOcrResponse>> recognizeLicensePlate(
            @RequestPart("image") MultipartFile image,
            @RequestParam(required = false) UUID laneId,
            @RequestParam(required = false) LaneDirection direction
    ) {
        LicensePlateOcrResult result = licensePlateOcrPortIn.recognize(new LicensePlateOcrCommand(
                image,
                laneId,
                direction
        ));
        return ResponseEntity.ok(ApiResponse.ok(
                "License plate recognized successfully",
                LicensePlateOcrResponse.from(result)
        ));
    }
}