package com.ban.vehicle_management.entrypoint.controller.parking;

import com.ban.vehicle_management.application.parking.location.mapper.ParkingLocationApiMapper;
import com.ban.vehicle_management.application.parking.location.port.in.PublicParkingLocationPortIn;
import com.ban.vehicle_management.entrypoint.dto.parking.location.response.ParkingLocationResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/parking-locations")
public class PublicParkingLocationController {

    private final PublicParkingLocationPortIn publicParkingLocationPortIn;
    private final ParkingLocationApiMapper parkingLocationApiMapper;

    public PublicParkingLocationController(
            PublicParkingLocationPortIn publicParkingLocationPortIn,
            ParkingLocationApiMapper parkingLocationApiMapper
    ) {
        this.publicParkingLocationPortIn = publicParkingLocationPortIn;
        this.parkingLocationApiMapper = parkingLocationApiMapper;
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ParkingLocationResponse>>> search(@RequestParam String query) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Public parking locations fetched successfully",
                parkingLocationApiMapper.toResponses(publicParkingLocationPortIn.search(query))));
    }

    @GetMapping("/reverse")
    public ResponseEntity<ApiResponse<List<ParkingLocationResponse>>> reverse(
            @RequestParam BigDecimal latitude,
            @RequestParam BigDecimal longitude
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Public parking location fetched successfully",
                parkingLocationApiMapper.toResponses(publicParkingLocationPortIn.reverse(latitude, longitude))));
    }
}
