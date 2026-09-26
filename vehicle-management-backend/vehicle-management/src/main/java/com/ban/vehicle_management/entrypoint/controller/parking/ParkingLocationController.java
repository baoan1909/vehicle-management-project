package com.ban.vehicle_management.entrypoint.controller.parking;

import com.ban.vehicle_management.application.parking.location.port.in.ParkingLocationPortIn;
import com.ban.vehicle_management.entrypoint.dto.parking.location.response.ParkingLocationResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parking/locations")
public class ParkingLocationController {

    private final ParkingLocationPortIn parkingLocationPortIn;

    public ParkingLocationController(ParkingLocationPortIn parkingLocationPortIn) {
        this.parkingLocationPortIn = parkingLocationPortIn;
    }

    @GetMapping("/search")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_LOT_CREATE_ALL')")
    public ResponseEntity<ApiResponse<List<ParkingLocationResponse>>> search(@RequestParam String query) {
        return ResponseEntity.ok(ApiResponse.ok("Parking locations fetched successfully", parkingLocationPortIn.search(query)
                .stream()
                .map(result -> new ParkingLocationResponse(result.displayName(), result.latitude(), result.longitude()))
                .toList()));
    }

    @GetMapping("/reverse")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_LOT_CREATE_ALL')")
    public ResponseEntity<ApiResponse<List<ParkingLocationResponse>>> reverse(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Parking location fetched successfully", parkingLocationPortIn.reverse(latitude, longitude)
                .stream()
                .map(result -> new ParkingLocationResponse(result.displayName(), result.latitude(), result.longitude()))
                .toList()));
    }
}
