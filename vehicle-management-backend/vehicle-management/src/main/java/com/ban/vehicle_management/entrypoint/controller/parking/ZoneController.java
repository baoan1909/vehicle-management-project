package com.ban.vehicle_management.entrypoint.controller.parking;

import com.ban.vehicle_management.application.parking.zone.mapper.ZoneApiMapper;
import com.ban.vehicle_management.application.parking.zone.port.in.ZonePortIn;
import com.ban.vehicle_management.domain.parking.zone.model.Zone;
import com.ban.vehicle_management.entrypoint.dto.parking.zone.request.CreateZoneRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.zone.request.UpdateZoneRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.zone.request.ZoneFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.zone.response.ZoneAdminResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parking/zones")
public class ZoneController {

    private final ZonePortIn zonePortIn;
    private final ZoneApiMapper zoneApiMapper;

    public ZoneController(ZonePortIn zonePortIn, ZoneApiMapper zoneApiMapper) {
        this.zonePortIn = zonePortIn;
        this.zoneApiMapper = zoneApiMapper;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ZoneAdminResponse>> createZone(@RequestBody CreateZoneRequest request) {
        Zone createdZone = zonePortIn.createZone(zoneApiMapper.toDomain(request));

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Zone created successfully",
                zoneApiMapper.toAdminResponse(createdZone)
        ));
    }

    @GetMapping("/{zoneId}")
    public ResponseEntity<ApiResponse<ZoneAdminResponse>> getZoneById(@PathVariable UUID zoneId) {
        Zone zone = zonePortIn.getZoneById(zoneId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched zone successfully",
                zoneApiMapper.toAdminResponse(zone)
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ZoneAdminResponse>>> getZones(@ModelAttribute ZoneFilterRequest request) {
        List<Zone> zones = zonePortIn.getZones(
                request.parkingLotId(),
                request.vehicleTypeId(),
                request.status(),
                request.keyword()
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched zones successfully",
                zoneApiMapper.toAdminResponses(zones)
        ));
    }

    @PutMapping("/{zoneId}")
    public ResponseEntity<ApiResponse<ZoneAdminResponse>> updateZone(
            @PathVariable UUID zoneId,
            @RequestBody UpdateZoneRequest request
    ) {
        Zone updatedZone = zonePortIn.updateZone(zoneId, zoneApiMapper.toDomain(request));

        return ResponseEntity.ok(ApiResponse.ok(
                "Zone updated successfully",
                zoneApiMapper.toAdminResponse(updatedZone)
        ));
    }

    @DeleteMapping("/{zoneId}")
    public ResponseEntity<ApiResponse<Void>> deleteZone(@PathVariable UUID zoneId) {
        zonePortIn.deleteZone(zoneId);

        return ResponseEntity.ok(ApiResponse.ok("Zone closed successfully"));
    }

    @PatchMapping("/{zoneId}/activate")
    public ResponseEntity<ApiResponse<ZoneAdminResponse>> activateZone(@PathVariable UUID zoneId) {
        Zone activatedZone = zonePortIn.activateZone(zoneId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Zone activated successfully",
                zoneApiMapper.toAdminResponse(activatedZone)
        ));
    }

    @PatchMapping("/{zoneId}/maintenance")
    public ResponseEntity<ApiResponse<ZoneAdminResponse>> markZoneMaintenance(@PathVariable UUID zoneId) {
        Zone zone = zonePortIn.markZoneMaintenance(zoneId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Zone marked as maintenance successfully",
                zoneApiMapper.toAdminResponse(zone)
        ));
    }

    @PatchMapping("/{zoneId}/close")
    public ResponseEntity<ApiResponse<ZoneAdminResponse>> closeZone(@PathVariable UUID zoneId) {
        Zone closedZone = zonePortIn.closeZone(zoneId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Zone closed successfully",
                zoneApiMapper.toAdminResponse(closedZone)
        ));
    }
}