package com.ban.vehicle_management.entrypoint.controller.parking;

import com.ban.vehicle_management.application.parking.parkinglevel.mapper.ParkingLevelApiMapper;
import com.ban.vehicle_management.application.parking.parkinglevel.port.in.ParkingLevelPortIn;
import com.ban.vehicle_management.application.parking.parkingspace.mapper.ParkingSpaceApiMapper;
import com.ban.vehicle_management.application.parking.parkingsummary.mapper.SpaceSummaryApiMapper;
import com.ban.vehicle_management.application.parking.parkingsummary.model.result.LevelMapResult;
import com.ban.vehicle_management.application.parking.parkingsummary.port.in.SpaceSummaryPortIn;
import com.ban.vehicle_management.application.parking.zone.mapper.ZoneApiMapper;
import com.ban.vehicle_management.domain.parking.parkinglevel.model.ParkingLevel;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglevel.request.CreateParkingLevelRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglevel.request.ParkingLevelFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglevel.request.UpdateParkingLevelRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglevel.response.LevelMapResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglevel.response.ParkingLevelAdminResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ParkingLevelController {

    private final ParkingLevelPortIn parkingLevelPortIn;
    private final SpaceSummaryPortIn spaceSummaryPortIn;
    private final ParkingLevelApiMapper parkingLevelApiMapper;
    private final ZoneApiMapper zoneApiMapper;
    private final ParkingSpaceApiMapper parkingSpaceApiMapper;
    private final SpaceSummaryApiMapper spaceSummaryApiMapper;

    public ParkingLevelController(
            ParkingLevelPortIn parkingLevelPortIn,
            SpaceSummaryPortIn spaceSummaryPortIn,
            ParkingLevelApiMapper parkingLevelApiMapper,
            ZoneApiMapper zoneApiMapper,
            ParkingSpaceApiMapper parkingSpaceApiMapper,
            SpaceSummaryApiMapper spaceSummaryApiMapper
    ) {
        this.parkingLevelPortIn = parkingLevelPortIn;
        this.spaceSummaryPortIn = spaceSummaryPortIn;
        this.parkingLevelApiMapper = parkingLevelApiMapper;
        this.zoneApiMapper = zoneApiMapper;
        this.parkingSpaceApiMapper = parkingSpaceApiMapper;
        this.spaceSummaryApiMapper = spaceSummaryApiMapper;
    }

    @PostMapping("/api/parking-lots/{lotId}/levels")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_CONFIGURE_ALL')")
    public ResponseEntity<ApiResponse<ParkingLevelAdminResponse>> createLevel(
            @PathVariable UUID lotId,
            @RequestBody CreateParkingLevelRequest request
    ) {
        ParkingLevel domain = parkingLevelApiMapper.toDomain(
                new CreateParkingLevelRequest(
                        lotId,
                        request.code(),
                        request.name(),
                        request.displayOrder(),
                        request.elevation(),
                        request.floorHeight(),
                        request.canvasWidth(),
                        request.canvasHeight()
                ));
        ParkingLevel created = parkingLevelPortIn.createParkingLevel(domain);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Parking level created successfully",
                parkingLevelApiMapper.toAdminResponse(created)
        ));
    }

    @GetMapping("/api/parking-lots/{lotId}/levels")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_READ_ALL')")
    public ResponseEntity<ApiResponse<List<ParkingLevelAdminResponse>>> getLevels(
            @PathVariable UUID lotId,
            @ModelAttribute ParkingLevelFilterRequest request
    ) {
        List<ParkingLevel> levels = parkingLevelPortIn.getParkingLevels(
                lotId, request.status(), request.keyword());
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched parking levels successfully",
                parkingLevelApiMapper.toAdminResponses(levels)
        ));
    }

    @GetMapping("/api/parking-levels/{levelId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_READ_ALL')")
    public ResponseEntity<ApiResponse<ParkingLevelAdminResponse>> getLevelById(
            @PathVariable UUID levelId
    ) {
        ParkingLevel level = parkingLevelPortIn.getParkingLevelById(levelId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched parking level successfully",
                parkingLevelApiMapper.toAdminResponse(level)
        ));
    }

    @PutMapping("/api/parking-levels/{levelId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_CONFIGURE_ALL')")
    public ResponseEntity<ApiResponse<ParkingLevelAdminResponse>> updateLevel(
            @PathVariable UUID levelId,
            @RequestBody UpdateParkingLevelRequest request
    ) {
        ParkingLevel updated = parkingLevelPortIn.updateParkingLevel(
                levelId, parkingLevelApiMapper.toDomain(request));
        return ResponseEntity.ok(ApiResponse.ok(
                "Parking level updated successfully",
                parkingLevelApiMapper.toAdminResponse(updated)
        ));
    }

    @DeleteMapping("/api/parking-levels/{levelId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_CONFIGURE_ALL')")
    public ResponseEntity<ApiResponse<Void>> deleteLevel(@PathVariable UUID levelId) {
        parkingLevelPortIn.deleteParkingLevel(levelId);
        return ResponseEntity.ok(ApiResponse.ok("Parking level deleted successfully"));
    }

    @PatchMapping("/api/parking-levels/{levelId}/activate")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_CONFIGURE_ALL')")
    public ResponseEntity<ApiResponse<ParkingLevelAdminResponse>> activateLevel(
            @PathVariable UUID levelId
    ) {
        ParkingLevel level = parkingLevelPortIn.activateParkingLevel(levelId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Parking level activated successfully",
                parkingLevelApiMapper.toAdminResponse(level)
        ));
    }

    @PatchMapping("/api/parking-levels/{levelId}/maintenance")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_CONFIGURE_ALL')")
    public ResponseEntity<ApiResponse<ParkingLevelAdminResponse>> markLevelMaintenance(
            @PathVariable UUID levelId
    ) {
        ParkingLevel level = parkingLevelPortIn.markParkingLevelMaintenance(levelId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Parking level marked as maintenance successfully",
                parkingLevelApiMapper.toAdminResponse(level)
        ));
    }

    @PatchMapping("/api/parking-levels/{levelId}/close")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_CONFIGURE_ALL')")
    public ResponseEntity<ApiResponse<ParkingLevelAdminResponse>> closeLevel(
            @PathVariable UUID levelId
    ) {
        ParkingLevel level = parkingLevelPortIn.closeParkingLevel(levelId);
        return ResponseEntity.ok(ApiResponse.ok(
                "Parking level closed successfully",
                parkingLevelApiMapper.toAdminResponse(level)
        ));
    }

    @GetMapping("/api/parking-levels/{levelId}/map")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_READ_ALL')")
    public ResponseEntity<ApiResponse<LevelMapResponse>> getLevelMap(
            @PathVariable UUID levelId
    ) {
        LevelMapResult result = spaceSummaryPortIn.getLevelMap(levelId);
        LevelMapResponse response = new LevelMapResponse();
        response.setLevel(parkingLevelApiMapper.toAdminResponse(result.level()));
        response.setZones(zoneApiMapper.toAdminResponses(result.zones()));
        response.setSpaces(parkingSpaceApiMapper.toAdminResponses(result.spaces()));
        response.setSummary(spaceSummaryApiMapper.toResponse(result.summary()));
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched level map successfully",
                response
        ));
    }
}
