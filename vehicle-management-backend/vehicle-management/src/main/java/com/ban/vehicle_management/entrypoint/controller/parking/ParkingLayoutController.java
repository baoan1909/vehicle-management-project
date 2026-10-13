package com.ban.vehicle_management.entrypoint.controller.parking;

import com.ban.vehicle_management.application.parking.parkinglayout.mapper.ParkingLayoutApiMapper;
import com.ban.vehicle_management.application.parking.parkinglayout.model.result.PublishPreviewResult;
import com.ban.vehicle_management.application.parking.parkinglayout.model.result.ZoneLayoutResult;
import com.ban.vehicle_management.application.parking.parkinglayout.port.in.ParkingLayoutPortIn;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglayout.request.PublishLayoutRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglayout.request.SaveLayoutRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglayout.response.PublishPreviewResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglayout.response.ZoneLayoutResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ParkingLayoutController {

    private final ParkingLayoutPortIn parkingLayoutPortIn;
    private final ParkingLayoutApiMapper parkingLayoutApiMapper;

    public ParkingLayoutController(
            ParkingLayoutPortIn parkingLayoutPortIn,
            ParkingLayoutApiMapper parkingLayoutApiMapper
    ) {
        this.parkingLayoutPortIn = parkingLayoutPortIn;
        this.parkingLayoutApiMapper = parkingLayoutApiMapper;
    }

    @PostMapping("/api/parking/zones/{zoneId}/layout/draft")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_CONFIGURE_ALL')")
    public ResponseEntity<ApiResponse<ZoneLayoutResponse>> createDraft(
            @PathVariable UUID zoneId
    ) {
        ZoneLayoutResult result = parkingLayoutPortIn.createDraftLayout(zoneId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Draft layout ready",
                parkingLayoutApiMapper.toResponse(result)
        ));
    }

    @GetMapping("/api/parking/zones/{zoneId}/layout/draft")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_READ_ALL')")
    public ResponseEntity<ApiResponse<ZoneLayoutResponse>> getDraft(
            @PathVariable UUID zoneId
    ) {
        ZoneLayoutResult result = parkingLayoutPortIn.getDraftLayout(zoneId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched draft layout successfully",
                parkingLayoutApiMapper.toResponse(result)
        ));
    }

    @PutMapping("/api/parking/zones/{zoneId}/layout/draft")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_CONFIGURE_ALL')")
    public ResponseEntity<ApiResponse<ZoneLayoutResponse>> saveDraft(
            @PathVariable UUID zoneId,
            @RequestBody SaveLayoutRequest request
    ) {
        ZoneLayoutResult result = parkingLayoutPortIn.saveLayout(
                zoneId,
                request.expectedDraftRevision(),
                parkingLayoutApiMapper.toItemCommands(
                        request.items() == null ? List.of() : request.items()),
                request.elements() == null
                        ? null
                        : parkingLayoutApiMapper.toElementCommands(request.elements()));
        return ResponseEntity.ok(ApiResponse.ok(
                "Draft layout saved successfully",
                parkingLayoutApiMapper.toResponse(result)
        ));
    }

    @DeleteMapping("/api/parking/zones/{zoneId}/layout/draft")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_CONFIGURE_ALL')")
    public ResponseEntity<ApiResponse<Void>> discardDraft(
            @PathVariable UUID zoneId,
            @RequestParam Long expectedDraftRevision
    ) {
        parkingLayoutPortIn.discardDraftLayout(zoneId, expectedDraftRevision);
        return ResponseEntity.ok(ApiResponse.ok("Draft layout discarded successfully"));
    }

    @GetMapping("/api/parking/zones/{zoneId}/layout/published")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_READ_ALL')")
    public ResponseEntity<ApiResponse<ZoneLayoutResponse>> getPublished(
            @PathVariable UUID zoneId
    ) {
        ZoneLayoutResult result = parkingLayoutPortIn.getPublishedLayout(zoneId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched published layout successfully",
                parkingLayoutApiMapper.toResponse(result)
        ));
    }

    @GetMapping("/api/parking/zones/{zoneId}/layout/publish-preview")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_READ_ALL')")
    public ResponseEntity<ApiResponse<PublishPreviewResponse>> previewPublish(
            @PathVariable UUID zoneId,
            @RequestParam Long expectedDraftRevision
    ) {
        PublishPreviewResult result = parkingLayoutPortIn.previewPublish(zoneId, expectedDraftRevision);
        return ResponseEntity.ok(ApiResponse.ok(
                "Publish preview calculated successfully",
                parkingLayoutApiMapper.toPreviewResponse(result)
        ));
    }

    @PostMapping("/api/parking/zones/{zoneId}/layout/publish")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_LAYOUT_PUBLISH_ALL')")
    public ResponseEntity<ApiResponse<ZoneLayoutResponse>> publish(
            @PathVariable UUID zoneId,
            @RequestBody(required = false) PublishLayoutRequest request
    ) {
        ZoneLayoutResult result = parkingLayoutPortIn.publishLayout(
                zoneId, request == null ? null : request.expectedDraftRevision());
        return ResponseEntity.ok(ApiResponse.ok(
                "Layout published successfully",
                parkingLayoutApiMapper.toResponse(result)
        ));
    }
}
