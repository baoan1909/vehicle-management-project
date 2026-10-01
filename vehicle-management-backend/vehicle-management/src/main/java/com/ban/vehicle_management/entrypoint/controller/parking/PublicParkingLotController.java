package com.ban.vehicle_management.entrypoint.controller.parking;

import com.ban.vehicle_management.application.parking.parkinglot.mapper.ParkingLotApiMapper;
import com.ban.vehicle_management.application.parking.parkinglot.port.in.NearbyParkingLotPortIn;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglot.response.ParkingLotPublicResponse;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingLotStatus;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/parking-lots")
public class PublicParkingLotController {

    private final ParkingLotPortOut parkingLotPortOut;
    private final NearbyParkingLotPortIn nearbyParkingLotPortIn;
    private final ParkingLotApiMapper parkingLotApiMapper;

    public PublicParkingLotController(
            ParkingLotPortOut parkingLotPortOut,
            NearbyParkingLotPortIn nearbyParkingLotPortIn,
            ParkingLotApiMapper parkingLotApiMapper
    ) {
        this.parkingLotPortOut = parkingLotPortOut;
        this.nearbyParkingLotPortIn = nearbyParkingLotPortIn;
        this.parkingLotApiMapper = parkingLotApiMapper;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ParkingLotPublicResponse>>> getActiveParkingLots() {
        return ResponseEntity.ok(ApiResponse.ok("Fetched active parking lots successfully",
                parkingLotApiMapper.toPublicResponses(
                        parkingLotPortOut.findAll(ParkingLotStatus.ACTIVE, null, null, null))));
    }

    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<ParkingLotPublicResponse>>> getNearbyParkingLots(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude,
            @RequestParam(defaultValue = "5") BigDecimal radiusKm,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched nearby parking lots successfully",
                parkingLotApiMapper.toNearbyPublicResponses(
                        nearbyParkingLotPortIn.findNearby(latitude, longitude, radiusKm, limit)
                )
        ));
    }
}
