package com.ban.vehicle_management.entrypoint.controller.parking;

import com.ban.vehicle_management.application.parking.location.mapper.ParkingLocationFeatureApiMapper;
import com.ban.vehicle_management.application.parking.location.port.in.ParkingLocationFeaturePortIn;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglot.response.ParkingMapFeatureResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/parking-map")
public class PublicParkingLocationFeatureController {

    private final ParkingLocationFeaturePortIn featurePortIn;
    private final ParkingLocationFeatureApiMapper featureApiMapper;

    public PublicParkingLocationFeatureController(
            ParkingLocationFeaturePortIn featurePortIn,
            ParkingLocationFeatureApiMapper featureApiMapper
    ) {
        this.featurePortIn = featurePortIn;
        this.featureApiMapper = featureApiMapper;
    }

    @GetMapping("/features")
    public ResponseEntity<ApiResponse<ParkingMapFeatureResponse>> getFeatures() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched parking map feature status",
                featureApiMapper.toResponse(featurePortIn.getPublicStatus())
        ));
    }
}