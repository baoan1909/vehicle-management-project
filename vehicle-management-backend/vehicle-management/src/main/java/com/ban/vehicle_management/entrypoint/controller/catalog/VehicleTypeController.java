package com.ban.vehicle_management.entrypoint.controller.catalog;

import com.ban.vehicle_management.application.catalog.vehicletype.mapper.VehicleTypeApiMapper;
import com.ban.vehicle_management.application.catalog.vehicletype.port.in.VehicleTypePortIn;
import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import com.ban.vehicle_management.entrypoint.dto.catalog.vehicletype.request.CreateVehicleTypeRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.vehicletype.request.VehicleTypeFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.vehicletype.request.UpdateVehicleTypeRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.vehicletype.response.VehicleTypeAdminResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog/vehicle-types")
public class VehicleTypeController {

    private final VehicleTypePortIn vehicleTypePortIn;
    private final VehicleTypeApiMapper vehicleTypeApiMapper;

    public VehicleTypeController(
            VehicleTypePortIn vehicleTypePortIn,
            VehicleTypeApiMapper vehicleTypeApiMapper
    ) {
        this.vehicleTypePortIn = vehicleTypePortIn;
        this.vehicleTypeApiMapper = vehicleTypeApiMapper;
    }

    @PostMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('VEHICLE_TYPE_CREATE_ALL')")
    public ResponseEntity<ApiResponse<VehicleTypeAdminResponse>> createVehicleType(@RequestBody CreateVehicleTypeRequest request) {
        VehicleType createdVehicleType = vehicleTypePortIn.createVehicleType(vehicleTypeApiMapper.toDomain(request));
        VehicleTypeAdminResponse response = vehicleTypeApiMapper.toAdminResponse(createdVehicleType);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Vehicle type created successfully", response));
    }

    @GetMapping("/{vehicleTypeId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('VEHICLE_TYPE_READ_ALL')")
    public ResponseEntity<ApiResponse<VehicleTypeAdminResponse>> getVehicleTypeById(@PathVariable UUID vehicleTypeId) {
        VehicleType vehicleType = vehicleTypePortIn.getVehicleTypeById(vehicleTypeId);
        VehicleTypeAdminResponse response = vehicleTypeApiMapper.toAdminResponse(vehicleType);
        return ResponseEntity.ok(ApiResponse.ok("Fetched vehicle type successfully", response));
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizer.hasAnyPermission('VEHICLE_TYPE_READ_ALL', 'PARKING_SESSION_CHECK_IN_ALL', 'PARKING_SESSION_CHECK_OUT_ALL')")
    public ResponseEntity<ApiResponse<List<VehicleTypeAdminResponse>>> getVehicleTypes(
            @ModelAttribute VehicleTypeFilterRequest request
    ) {
        List<VehicleType> vehicleTypes = vehicleTypePortIn.getVehicleTypes(request.isActive());
        List<VehicleTypeAdminResponse> response = vehicleTypeApiMapper.toAdminResponses(vehicleTypes);
        return ResponseEntity.ok(ApiResponse.ok("Fetched vehicle types successfully", response));
    }

    @PutMapping("/{vehicleTypeId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('VEHICLE_TYPE_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<VehicleTypeAdminResponse>> updateVehicleType(
            @PathVariable UUID vehicleTypeId,
            @RequestBody UpdateVehicleTypeRequest request
    ) {
        VehicleType updatedVehicleType = vehicleTypePortIn.updateVehicleType(
                vehicleTypeId,
                vehicleTypeApiMapper.toDomain(request)
        );
        VehicleTypeAdminResponse response = vehicleTypeApiMapper.toAdminResponse(updatedVehicleType);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle type updated successfully", response));
    }

    @DeleteMapping("/{vehicleTypeId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('VEHICLE_TYPE_DELETE_ALL')")
    public ResponseEntity<ApiResponse<Void>> deleteVehicleType(@PathVariable UUID vehicleTypeId) {
        vehicleTypePortIn.deleteVehicleType(vehicleTypeId);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle type deactivated successfully"));
    }

    @PatchMapping("/{vehicleTypeId}/activate")
    @PreAuthorize("@permissionAuthorizer.hasPermission('VEHICLE_TYPE_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<VehicleTypeAdminResponse>> activateVehicleType(@PathVariable UUID vehicleTypeId) {
        VehicleType vehicleType = vehicleTypePortIn.activateVehicleType(vehicleTypeId);
        VehicleTypeAdminResponse response = vehicleTypeApiMapper.toAdminResponse(vehicleType);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle type activated successfully", response));
    }
}


