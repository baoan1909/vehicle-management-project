package com.ban.vehicle_management.entrypoint.controller.parking;

import com.ban.vehicle_management.application.parking.gate.mapper.GateApiMapper;
import com.ban.vehicle_management.application.parking.gate.port.in.GatePortIn;
import com.ban.vehicle_management.domain.parking.gate.model.Gate;
import com.ban.vehicle_management.entrypoint.dto.parking.gate.request.CreateGateRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.gate.request.GateFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.gate.request.UpdateGateRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.gate.response.GateAdminResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/parking/gates")
public class GateController {

    private final GatePortIn gatePortIn;
    private final GateApiMapper gateApiMapper;

    public GateController(GatePortIn gatePortIn, GateApiMapper gateApiMapper) {
        this.gatePortIn = gatePortIn;
        this.gateApiMapper = gateApiMapper;
    }

    @PostMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_CONFIGURE_ALL')")
    public ResponseEntity<ApiResponse<GateAdminResponse>> createGate(@RequestBody CreateGateRequest request) {
        Gate createdGate = gatePortIn.createGate(gateApiMapper.toDomain(request));

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Gate created successfully",
                gateApiMapper.toAdminResponse(createdGate)
        ));
    }

    @GetMapping("/{gateId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_READ_ALL')")
    public ResponseEntity<ApiResponse<GateAdminResponse>> getGateById(@PathVariable UUID gateId) {
        Gate gate = gatePortIn.getGateById(gateId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched gate successfully",
                gateApiMapper.toAdminResponse(gate)
        ));
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_READ_ALL')")
    public ResponseEntity<ApiResponse<List<GateAdminResponse>>> getGates(@ModelAttribute GateFilterRequest request) {
        List<Gate> gates = gatePortIn.getGates(request.zoneId(), request.status(), request.keyword());

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched gates successfully",
                gateApiMapper.toAdminResponses(gates)
        ));
    }

    @PutMapping("/{gateId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_CONFIGURE_ALL')")
    public ResponseEntity<ApiResponse<GateAdminResponse>> updateGate(
            @PathVariable UUID gateId,
            @RequestBody UpdateGateRequest request
    ) {
        Gate updatedGate = gatePortIn.updateGate(gateId, gateApiMapper.toDomain(request));

        return ResponseEntity.ok(ApiResponse.ok(
                "Gate updated successfully",
                gateApiMapper.toAdminResponse(updatedGate)
        ));
    }

    @DeleteMapping("/{gateId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_CONFIGURE_ALL')")
    public ResponseEntity<ApiResponse<Void>> deleteGate(@PathVariable UUID gateId) {
        gatePortIn.deleteGate(gateId);

        return ResponseEntity.ok(ApiResponse.ok("Gate closed successfully"));
    }

    @PatchMapping("/{gateId}/activate")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_CONFIGURE_ALL')")
    public ResponseEntity<ApiResponse<GateAdminResponse>> activateGate(@PathVariable UUID gateId) {
        Gate activatedGate = gatePortIn.activateGate(gateId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Gate activated successfully",
                gateApiMapper.toAdminResponse(activatedGate)
        ));
    }

    @PatchMapping("/{gateId}/maintenance")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_CONFIGURE_ALL')")
    public ResponseEntity<ApiResponse<GateAdminResponse>> markGateMaintenance(@PathVariable UUID gateId) {
        Gate gate = gatePortIn.markGateMaintenance(gateId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Gate marked as maintenance successfully",
                gateApiMapper.toAdminResponse(gate)
        ));
    }

    @PatchMapping("/{gateId}/close")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SPACE_CONFIGURE_ALL')")
    public ResponseEntity<ApiResponse<GateAdminResponse>> closeGate(@PathVariable UUID gateId) {
        Gate closedGate = gatePortIn.closeGate(gateId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Gate closed successfully",
                gateApiMapper.toAdminResponse(closedGate)
        ));
    }
}