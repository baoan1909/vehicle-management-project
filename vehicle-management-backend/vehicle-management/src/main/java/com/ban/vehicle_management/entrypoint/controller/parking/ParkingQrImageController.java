package com.ban.vehicle_management.entrypoint.controller.parking;

import com.ban.vehicle_management.application.parking.parkingsession.port.in.ParkingQrImagePortIn;
import com.ban.vehicle_management.application.parking.parkingsession.mapper.ParkingQrImageApiMapper;
import com.ban.vehicle_management.entrypoint.converter.MultipartFileBytesConverter;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.response.ParkingQrImageResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/parking/parking-sessions/qr")
public class ParkingQrImageController {
    private final ParkingQrImagePortIn qrImages;
    private final ParkingQrImageApiMapper mapper;
    private final MultipartFileBytesConverter fileBytesConverter;

    public ParkingQrImageController(ParkingQrImagePortIn qrImages,
            ParkingQrImageApiMapper mapper,
            MultipartFileBytesConverter fileBytesConverter) {
        this.qrImages = qrImages;
        this.mapper = mapper;
        this.fileBytesConverter = fileBytesConverter;
    }

    @PostMapping(value = "/decode-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permissionAuthorizer.hasAnyPermission('PARKING_SESSION_CHECK_IN_ALL', 'PARKING_SESSION_CHECK_OUT_ALL')")
    public ResponseEntity<ApiResponse<ParkingQrImageResponse>> decodeImage(
            @RequestPart("image") MultipartFile image,
            @RequestParam UUID parkingLotId) {
        return ResponseEntity.ok(ApiResponse.ok("Parking QR image decoded",
                mapper.toResponse(qrImages.decodeForOperation(
                        fileBytesConverter.read(image, "Unable to read QR image"), parkingLotId))));
    }
}
