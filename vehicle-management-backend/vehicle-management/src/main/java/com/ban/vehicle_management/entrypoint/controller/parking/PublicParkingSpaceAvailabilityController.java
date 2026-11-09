package com.ban.vehicle_management.entrypoint.controller.parking;

import com.ban.vehicle_management.application.parking.parkingsummary.model.result.PublicAvailabilityResult;
import com.ban.vehicle_management.application.parking.parkingsummary.port.in.PublicSpaceAvailabilityPortIn;
import com.ban.vehicle_management.application.parking.parkingsummary.mapper.SpaceSummaryApiMapper;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsummary.response.PublicAvailabilityResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/parking-lots")
public class PublicParkingSpaceAvailabilityController {

    private final PublicSpaceAvailabilityPortIn availabilityPortIn;
    private final SpaceSummaryApiMapper mapper;

    public PublicParkingSpaceAvailabilityController(
            PublicSpaceAvailabilityPortIn availabilityPortIn,
            SpaceSummaryApiMapper mapper
    ) {
        this.availabilityPortIn = availabilityPortIn;
        this.mapper = mapper;
    }

    @GetMapping("/{parkingLotId}/space-availability")
    public ResponseEntity<ApiResponse<PublicAvailabilityResponse>> getSpaceAvailability(
            @PathVariable UUID parkingLotId
    ) {
        PublicAvailabilityResult result = availabilityPortIn.getPublicAvailability(parkingLotId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched parking space availability successfully",
                mapper.toResponse(result)));
    }
}
