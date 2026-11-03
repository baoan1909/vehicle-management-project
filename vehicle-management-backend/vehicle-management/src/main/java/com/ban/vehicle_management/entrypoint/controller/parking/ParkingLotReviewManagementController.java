package com.ban.vehicle_management.entrypoint.controller.parking;

import com.ban.vehicle_management.application.parking.parkinglotreview.mapper.ParkingLotReviewApiMapper;
import com.ban.vehicle_management.application.parking.parkinglotreview.port.in.ManageParkingLotReviewPortIn;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglotreview.response.ParkingLotReviewPageResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.time.Instant;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parking/parking-lot-reviews")
public class ParkingLotReviewManagementController {
    private final ManageParkingLotReviewPortIn portIn;
    private final ParkingLotReviewApiMapper mapper;

    public ParkingLotReviewManagementController(ManageParkingLotReviewPortIn portIn,
                                                ParkingLotReviewApiMapper mapper) {
        this.portIn = portIn;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ParkingLotReviewPageResponse>> getReviews(
            @RequestParam(required = false) UUID parkingLotId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok("Fetched scoped parking lot reviews successfully",
                mapper.toManagementPageResponse(portIn.getReviews(parkingLotId, rating, from, to, page, size))));
    }
}
