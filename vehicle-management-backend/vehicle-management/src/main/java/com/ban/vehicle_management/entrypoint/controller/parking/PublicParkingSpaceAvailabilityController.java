package com.ban.vehicle_management.entrypoint.controller.parking;

import com.ban.vehicle_management.application.parking.parkingsummary.model.result.PublicAvailabilityResult;
import com.ban.vehicle_management.application.parking.parkingsummary.port.in.PublicSpaceAvailabilityPortIn;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsummary.response.PublicAvailabilityResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
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

    public PublicParkingSpaceAvailabilityController(PublicSpaceAvailabilityPortIn availabilityPortIn) {
        this.availabilityPortIn = availabilityPortIn;
    }

    @GetMapping("/{parkingLotId}/space-availability")
    public ResponseEntity<ApiResponse<PublicAvailabilityResponse>> getSpaceAvailability(
            @PathVariable UUID parkingLotId
    ) {
        PublicAvailabilityResult result = availabilityPortIn.getPublicAvailability(parkingLotId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched parking space availability successfully",
                toResponse(result)));
    }

    private PublicAvailabilityResponse toResponse(PublicAvailabilityResult result) {
        PublicAvailabilityResponse response = new PublicAvailabilityResponse();
        response.setParkingLotId(result.parkingLotId());
        response.setParkingLotName(result.parkingLotName());
        response.setParkingLotStatus(result.parkingLotStatus());
        response.setTotal(result.total());
        response.setVisitorAvailable(result.visitorAvailable());
        response.setSubscriptionAvailable(result.subscriptionAvailable());
        response.setHasAvailability(result.hasAvailability());
        response.setSnapshotAt(result.snapshotAt());
        response.setBreakdown(result.breakdown() == null ? List.of() : result.breakdown().stream()
                .map(item -> {
                    PublicAvailabilityResponse.VehicleTypeAvailabilityResponse entry =
                            new PublicAvailabilityResponse.VehicleTypeAvailabilityResponse();
                    entry.setVehicleTypeId(item.vehicleTypeId());
                    entry.setTotal(item.total());
                    entry.setVisitorAvailable(item.visitorAvailable());
                    entry.setSubscriptionAvailable(item.subscriptionAvailable());
                    return entry;
                })
                .toList());
        return response;
    }
}
