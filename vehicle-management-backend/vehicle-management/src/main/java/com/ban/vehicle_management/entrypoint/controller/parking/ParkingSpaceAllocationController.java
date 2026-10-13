package com.ban.vehicle_management.entrypoint.controller.parking;

import com.ban.vehicle_management.application.parking.parkingspaceallocation.mapper.ParkingSpaceAllocationApiMapper;
import com.ban.vehicle_management.application.parking.parkingspaceallocation.port.in.ParkingSpaceAllocationPortIn;
import com.ban.vehicle_management.domain.parking.parkingspaceallocation.model.ParkingSpaceAllocation;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingspaceallocation.request.CreateParkingSpaceAllocationRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingspaceallocation.request.UpdateAllocationHoldRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingspaceallocation.response.ParkingSpaceAllocationResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ParkingSpaceAllocationController {

    private final ParkingSpaceAllocationPortIn allocationPortIn;
    private final ParkingSpaceAllocationApiMapper mapper;

    public ParkingSpaceAllocationController(
            ParkingSpaceAllocationPortIn allocationPortIn,
            ParkingSpaceAllocationApiMapper mapper
    ) {
        this.allocationPortIn = allocationPortIn;
        this.mapper = mapper;
    }

    @PostMapping("/api/parking/spaces/{spaceId}/allocations")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_ASSIGN_ALL')")
    public ResponseEntity<ApiResponse<ParkingSpaceAllocationResponse>> create(
            @PathVariable UUID spaceId,
            @RequestBody CreateParkingSpaceAllocationRequest request
    ) {
        ParkingSpaceAllocation allocation = allocationPortIn.createAllocation(
                mapper.toDomain(request, spaceId));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Parking space allocation created successfully", mapper.toResponse(allocation)));
    }

    @GetMapping("/api/parking/allocations/{allocationId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_READ_ALL')")
    public ResponseEntity<ApiResponse<ParkingSpaceAllocationResponse>> getById(
            @PathVariable UUID allocationId
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Fetched parking space allocation successfully",
                mapper.toResponse(allocationPortIn.getAllocationById(allocationId))));
    }

    @GetMapping("/api/parking/spaces/{spaceId}/allocations")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_READ_ALL')")
    public ResponseEntity<ApiResponse<List<ParkingSpaceAllocationResponse>>> getBySpace(
            @PathVariable UUID spaceId,
            @RequestParam(required = false) LocalDate activeOn
    ) {
        List<ParkingSpaceAllocation> allocations = activeOn == null
                ? allocationPortIn.getAllocationsByParkingSpaceId(spaceId)
                : allocationPortIn.findActiveAllocationsForSpaceOnDate(spaceId, activeOn);
        return ResponseEntity.ok(ApiResponse.ok("Fetched parking space allocations successfully",
                mapper.toResponses(allocations)));
    }

    @GetMapping("/api/parking/zones/{zoneId}/allocations")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_READ_ALL')")
    public ResponseEntity<ApiResponse<List<ParkingSpaceAllocationResponse>>> getEffectiveByZone(
            @PathVariable UUID zoneId,
            @RequestParam(required = false) LocalDate activeOn
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Fetched zone parking space allocations successfully",
                mapper.toResponses(allocationPortIn.findEffectiveAllocationsByZone(zoneId, activeOn))));
    }

    @GetMapping("/api/subscriptions/{subscriptionId}/parking-space-allocations")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_READ_ALL')")
    public ResponseEntity<ApiResponse<List<ParkingSpaceAllocationResponse>>> getBySubscription(
            @PathVariable UUID subscriptionId,
            @RequestParam(defaultValue = "false") boolean activeOnly
    ) {
        List<ParkingSpaceAllocation> allocations = activeOnly
                ? allocationPortIn.findActiveAllocationsBySubscription(subscriptionId)
                : allocationPortIn.getAllocationsBySubscriptionId(subscriptionId);
        return ResponseEntity.ok(ApiResponse.ok("Fetched subscription parking space allocations successfully",
                mapper.toResponses(allocations)));
    }

    @PatchMapping("/api/parking/allocations/{allocationId}/activate")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_ASSIGN_ALL')")
    public ResponseEntity<ApiResponse<ParkingSpaceAllocationResponse>> activate(
            @PathVariable UUID allocationId
    ) {
        return updated("Parking space allocation activated successfully",
                allocationPortIn.activateAllocation(allocationId));
    }

    @PatchMapping("/api/parking/allocations/{allocationId}/release")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_ASSIGN_ALL')")
    public ResponseEntity<ApiResponse<ParkingSpaceAllocationResponse>> release(
            @PathVariable UUID allocationId
    ) {
        return updated("Parking space allocation released successfully",
                allocationPortIn.releaseAllocation(allocationId));
    }

    @PatchMapping("/api/parking/allocations/{allocationId}/cancel")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_ASSIGN_ALL')")
    public ResponseEntity<ApiResponse<ParkingSpaceAllocationResponse>> cancel(
            @PathVariable UUID allocationId
    ) {
        return updated("Parking space allocation cancelled successfully",
                allocationPortIn.cancelAllocation(allocationId));
    }

    @PatchMapping("/api/parking/allocations/{allocationId}/hold-expiration")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_ASSIGN_ALL')")
    public ResponseEntity<ApiResponse<ParkingSpaceAllocationResponse>> setHoldExpiration(
            @PathVariable UUID allocationId,
            @RequestBody UpdateAllocationHoldRequest request
    ) {
        return updated("Parking space allocation hold updated successfully",
                allocationPortIn.setHoldExpiration(allocationId, request.holdExpiresAt()));
    }

    private ResponseEntity<ApiResponse<ParkingSpaceAllocationResponse>> updated(
            String message, ParkingSpaceAllocation allocation) {
        return ResponseEntity.ok(ApiResponse.ok(message, mapper.toResponse(allocation)));
    }
}
