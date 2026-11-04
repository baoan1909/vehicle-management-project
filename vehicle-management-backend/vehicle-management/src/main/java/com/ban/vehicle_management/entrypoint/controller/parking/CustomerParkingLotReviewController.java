package com.ban.vehicle_management.entrypoint.controller.parking;

import com.ban.vehicle_management.application.parking.parkinglotreview.mapper.ParkingLotReviewApiMapper;
import com.ban.vehicle_management.application.parking.parkinglotreview.port.in.CustomerParkingLotReviewPortIn;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglotreview.request.UpsertParkingLotReviewRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglotreview.response.MyParkingLotReviewResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parking/parking-lots/{parkingLotId}/reviews/me")
public class CustomerParkingLotReviewController {
    private final CustomerParkingLotReviewPortIn portIn;
    private final ParkingLotReviewApiMapper mapper;

    public CustomerParkingLotReviewController(CustomerParkingLotReviewPortIn portIn,
                                               ParkingLotReviewApiMapper mapper) {
        this.portIn = portIn;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<MyParkingLotReviewResponse>> getMyReview(
            @PathVariable UUID parkingLotId) {
        return ResponseEntity.ok(ApiResponse.ok("Fetched review eligibility successfully",
                mapper.toMyResponse(portIn.getMyReview(parkingLotId))));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<MyParkingLotReviewResponse.CurrentReview>> upsert(
            @PathVariable UUID parkingLotId,
            @Valid @RequestBody UpsertParkingLotReviewRequest request) {
        var result = portIn.upsert(parkingLotId, request.rating(), request.comment());
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(ApiResponse.ok(
                result.created() ? "Created parking lot review successfully" : "Updated parking lot review successfully",
                mapper.toCurrentReview(result.review())));
    }
}
