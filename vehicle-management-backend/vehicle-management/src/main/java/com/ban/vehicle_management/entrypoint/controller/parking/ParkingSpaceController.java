package com.ban.vehicle_management.entrypoint.controller.parking;

import com.ban.vehicle_management.application.parking.parkingspace.mapper.ParkingSpaceApiMapper;
import com.ban.vehicle_management.application.parking.parkingspace.port.in.ParkingSpacePortIn;
import com.ban.vehicle_management.application.parking.parkingsession.mapper.ParkingSessionApiMapper;
import com.ban.vehicle_management.application.parking.parkingsummary.mapper.SpaceSummaryApiMapper;
import com.ban.vehicle_management.application.parking.parkingsummary.model.result.SpaceReconciliationIssue;
import com.ban.vehicle_management.application.parking.parkingsummary.port.in.SpaceSummaryPortIn;
import com.ban.vehicle_management.domain.parking.parkingspace.model.ParkingSpace;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingspace.request.BulkCreateParkingSpaceRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingspace.request.CreateParkingSpaceRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingspace.request.UpdateParkingSpaceRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingspace.request.UpdateParkingSpaceStatusRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingspace.response.ParkingSpaceAdminResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingspace.response.ParkingSpaceSummaryResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.response.ParkingSessionResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ParkingSpaceController {

    private final ParkingSpacePortIn parkingSpacePortIn;
    private final SpaceSummaryPortIn spaceSummaryPortIn;
    private final ParkingSpaceApiMapper parkingSpaceApiMapper;
    private final ParkingSessionApiMapper parkingSessionApiMapper;
    private final SpaceSummaryApiMapper spaceSummaryApiMapper;

    public ParkingSpaceController(
            ParkingSpacePortIn parkingSpacePortIn,
            SpaceSummaryPortIn spaceSummaryPortIn,
            ParkingSpaceApiMapper parkingSpaceApiMapper,
            ParkingSessionApiMapper parkingSessionApiMapper,
            SpaceSummaryApiMapper spaceSummaryApiMapper
    ) {
        this.parkingSpacePortIn = parkingSpacePortIn;
        this.spaceSummaryPortIn = spaceSummaryPortIn;
        this.parkingSpaceApiMapper = parkingSpaceApiMapper;
        this.parkingSessionApiMapper = parkingSessionApiMapper;
        this.spaceSummaryApiMapper = spaceSummaryApiMapper;
    }

    @PostMapping("/api/parking/zones/{zoneId}/spaces")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_CONFIGURE_ALL')")
    public ResponseEntity<ApiResponse<ParkingSpaceAdminResponse>> createSpace(
            @PathVariable UUID zoneId,
            @RequestBody CreateParkingSpaceRequest request
    ) {
        ParkingSpace created = parkingSpacePortIn.createParkingSpace(
                parkingSpaceApiMapper.toDomain(attachZone(zoneId, request)));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Parking space created successfully",
                parkingSpaceApiMapper.toAdminResponse(created)
        ));
    }

    @PostMapping("/api/parking/zones/{zoneId}/spaces/bulk")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_CONFIGURE_ALL')")
    public ResponseEntity<ApiResponse<List<ParkingSpaceAdminResponse>>> bulkCreateSpaces(
            @PathVariable UUID zoneId,
            @RequestBody BulkCreateParkingSpaceRequest request
    ) {
        List<CreateParkingSpaceRequest> items = request.spaces() == null ? List.of() : request.spaces();
        List<ParkingSpace> created = parkingSpacePortIn.bulkCreateParkingSpaces(
                items.stream().map(item -> parkingSpaceApiMapper.toDomain(attachZone(zoneId, item))).toList());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Parking spaces created successfully",
                parkingSpaceApiMapper.toAdminResponses(created)
        ));
    }

    @GetMapping("/api/parking/zones/{zoneId}/spaces")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_READ_ALL')")
    public ResponseEntity<ApiResponse<List<ParkingSpaceAdminResponse>>> getSpaces(
            @PathVariable UUID zoneId,
            @RequestParam(required = false) com.ban.vehicle_management.shared.enumeration.parking.ParkingSpaceStatus status,
            @RequestParam(required = false) String lifecycleStatus
    ) {
        List<ParkingSpace> spaces = parkingSpacePortIn.getParkingSpaces(zoneId, status, lifecycleStatus);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched parking spaces successfully",
                parkingSpaceApiMapper.toAdminResponses(spaces)
        ));
    }

    @GetMapping("/api/parking/spaces/{spaceId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_READ_ALL')")
    public ResponseEntity<ApiResponse<ParkingSpaceAdminResponse>> getSpaceById(
            @PathVariable UUID spaceId
    ) {
        ParkingSpace space = parkingSpacePortIn.getParkingSpaceById(spaceId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched parking space successfully",
                parkingSpaceApiMapper.toAdminResponse(space)
        ));
    }

    @PutMapping("/api/parking/spaces/{spaceId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_CONFIGURE_ALL')")
    public ResponseEntity<ApiResponse<ParkingSpaceAdminResponse>> updateSpace(
            @PathVariable UUID spaceId,
            @RequestBody UpdateParkingSpaceRequest request
    ) {
        ParkingSpace updated = parkingSpacePortIn.updateParkingSpace(
                spaceId, parkingSpaceApiMapper.toDomain(request));
        return ResponseEntity.ok(ApiResponse.ok(
                "Parking space updated successfully",
                parkingSpaceApiMapper.toAdminResponse(updated)
        ));
    }

    @DeleteMapping("/api/parking/spaces/{spaceId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_CONFIGURE_ALL')")
    public ResponseEntity<ApiResponse<Void>> archiveSpace(@PathVariable UUID spaceId) {
        parkingSpacePortIn.deleteParkingSpace(spaceId);
        return ResponseEntity.ok(ApiResponse.ok("Parking space archived successfully"));
    }

    @PatchMapping("/api/parking/spaces/{spaceId}/status")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_STATUS_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<ParkingSpaceAdminResponse>> updateSpaceStatus(
            @PathVariable UUID spaceId,
            @RequestBody UpdateParkingSpaceStatusRequest request
    ) {
        ParkingSpace updated = parkingSpacePortIn.setManualStatus(
                spaceId, request.targetStatus(), request.occupancyType(), request.reason());
        return ResponseEntity.ok(ApiResponse.ok(
                "Parking space status updated successfully",
                parkingSpaceApiMapper.toAdminResponse(updated)
        ));
    }

    @GetMapping("/api/parking-lots/{lotId}/space-summary")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_READ_ALL')")
    public ResponseEntity<ApiResponse<ParkingSpaceSummaryResponse>> getLotSummary(
            @PathVariable UUID lotId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched lot space summary successfully",
                spaceSummaryApiMapper.toResponse(spaceSummaryPortIn.getLotSummary(lotId))
        ));
    }

    @GetMapping("/api/parking-levels/{levelId}/space-summary")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_READ_ALL')")
    public ResponseEntity<ApiResponse<ParkingSpaceSummaryResponse>> getLevelSummary(
            @PathVariable UUID levelId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched level space summary successfully",
                spaceSummaryApiMapper.toResponse(spaceSummaryPortIn.getLevelSummary(levelId))
        ));
    }

    @GetMapping("/api/parking/zones/{zoneId}/space-summary")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_READ_ALL')")
    public ResponseEntity<ApiResponse<ParkingSpaceSummaryResponse>> getZoneSummary(
            @PathVariable UUID zoneId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched zone space summary successfully",
                spaceSummaryApiMapper.toResponse(spaceSummaryPortIn.getZoneSummary(zoneId))
        ));
    }

    @GetMapping("/api/parking-lots/{lotId}/space-reconciliation")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_READ_ALL')")
    public ResponseEntity<ApiResponse<List<SpaceReconciliationIssue>>> getLotReconciliation(
            @PathVariable UUID lotId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched lot space reconciliation successfully",
                spaceSummaryPortIn.findLotReconciliationIssues(lotId)
        ));
    }

    @GetMapping("/api/parking/zones/{zoneId}/space-reconciliation")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_READ_ALL')")
    public ResponseEntity<ApiResponse<List<SpaceReconciliationIssue>>> getZoneReconciliation(
            @PathVariable UUID zoneId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched zone space reconciliation successfully",
                spaceSummaryPortIn.findZoneReconciliationIssues(zoneId)
        ));
    }

    @GetMapping("/api/parking/spaces/{spaceId}/open-session")
    @PreAuthorize("@permissionAuthorizer.hasAnyPermission('PARKING_SPACE_READ_ALL', 'PARKING_SESSION_CHECK_IN_ALL', 'PARKING_SESSION_CHECK_OUT_ALL')")
    public ResponseEntity<ApiResponse<ParkingSessionResponse>> getOpenSessionBySpace(
            @PathVariable UUID spaceId
    ) {
        return parkingSpacePortIn.findOpenSessionBySpace(spaceId)
                .map(session -> ResponseEntity.ok(ApiResponse.ok(
                        "Fetched open parking session successfully",
                        parkingSessionApiMapper.toResponse(session))))
                .orElseThrow(() -> new com.ban.vehicle_management.shared.exception.NotFoundException(
                        "No open parking session for this space"));
    }

    private CreateParkingSpaceRequest attachZone(UUID zoneId, CreateParkingSpaceRequest request) {
        return new CreateParkingSpaceRequest(
                zoneId,
                request.code(),
                request.vehicleTypeId(),
                request.x(),
                request.y(),
                request.width(),
                request.height(),
                request.rotation()
        );
    }
}
