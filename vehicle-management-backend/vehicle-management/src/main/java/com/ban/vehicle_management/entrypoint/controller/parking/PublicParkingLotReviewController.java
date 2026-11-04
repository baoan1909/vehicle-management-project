package com.ban.vehicle_management.entrypoint.controller.parking;

import com.ban.vehicle_management.application.parking.parkinglotreview.mapper.ParkingLotReviewApiMapper;
import com.ban.vehicle_management.application.parking.parkinglotreview.port.in.PublicParkingLotReviewPortIn;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglotreview.response.ParkingLotReviewPageResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/parking-lots/{parkingLotId}/reviews")
public class PublicParkingLotReviewController {
    private final PublicParkingLotReviewPortIn portIn;
    private final ParkingLotReviewApiMapper mapper;

    public PublicParkingLotReviewController(PublicParkingLotReviewPortIn portIn,
                                            ParkingLotReviewApiMapper mapper) {
        this.portIn = portIn;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ParkingLotReviewPageResponse>> getReviews(
            @PathVariable UUID parkingLotId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok("Fetched parking lot reviews successfully",
                mapper.toPublicPageResponse(portIn.getReviews(parkingLotId, rating, page, size))));
    }
}
