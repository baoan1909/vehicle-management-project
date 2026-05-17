package com.ban.vehicle_management.entrypoint.controller.catalog;

import com.ban.vehicle_management.application.catalog.vehicletype.mapper.VehicleTypeApiMapper;
import com.ban.vehicle_management.application.catalog.vehicletype.port.in.VehicleTypePortIn;
import com.ban.vehicle_management.domain.catalog.vehicletype.model.VehicleType;
import com.ban.vehicle_management.entrypoint.dto.catalog.vehicletype.request.CreateVehicleTypeRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.vehicletype.request.UpdateVehicleTypeRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.vehicletype.response.VehicleTypeAdminResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog/vehicle-types")
public class VehicleTypeController {

    private final VehicleTypePortIn vehicleTypeUseCase;
    private final VehicleTypeApiMapper vehicleTypeApiMapper;

    public VehicleTypeController(
            VehicleTypePortIn vehicleTypeUseCase,
            VehicleTypeApiMapper vehicleTypeApiMapper
    ) {
        this.vehicleTypeUseCase = vehicleTypeUseCase;
        this.vehicleTypeApiMapper = vehicleTypeApiMapper;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleTypeAdminResponse>> createVehicleType(@RequestBody CreateVehicleTypeRequest request) {
        VehicleType createdVehicleType = vehicleTypeUseCase.createVehicleType(vehicleTypeApiMapper.toDomain(request));
        VehicleTypeAdminResponse response = vehicleTypeApiMapper.toAdminResponse(createdVehicleType);

        return ResponseEntity.ok(ApiResponse.ok("Vehicle type created successfully", response));
    }

    @GetMapping("/{vehicleTypeId}")
    public ResponseEntity<ApiResponse<VehicleTypeAdminResponse>> getVehicleTypeById(@PathVariable UUID vehicleTypeId) {
        VehicleType vehicleType = vehicleTypeUseCase.getVehicleTypeById(vehicleTypeId);
        VehicleTypeAdminResponse response = vehicleTypeApiMapper.toAdminResponse(vehicleType);
        return ResponseEntity.ok(ApiResponse.ok("Fetched vehicle type successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<VehicleTypeAdminResponse>>> getVehicleTypes(
            @RequestParam(required = false) Boolean isActive
    ) {
        List<VehicleType> vehicleTypes = vehicleTypeUseCase.getVehicleTypes(isActive);
        List<VehicleTypeAdminResponse> response = vehicleTypeApiMapper.toAdminResponses(vehicleTypes);
        return ResponseEntity.ok(ApiResponse.ok("Fetched vehicle types successfully", response));
    }

    @PutMapping("/{vehicleTypeId}")
    public ResponseEntity<ApiResponse<VehicleTypeAdminResponse>> updateVehicleType(
            @PathVariable UUID vehicleTypeId,
            @RequestBody UpdateVehicleTypeRequest request
    ) {
        VehicleType updatedVehicleType = vehicleTypeUseCase.updateVehicleType(
                vehicleTypeId,
                vehicleTypeApiMapper.toDomain(request)
        );
        VehicleTypeAdminResponse response = vehicleTypeApiMapper.toAdminResponse(updatedVehicleType);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle type updated successfully", response));
    }

    @DeleteMapping("/{vehicleTypeId}")
    public ResponseEntity<ApiResponse<Void>> deleteVehicleType(@PathVariable UUID vehicleTypeId) {
        vehicleTypeUseCase.deleteVehicleType(vehicleTypeId);
        return ResponseEntity.ok(ApiResponse.ok("Vehicle type deactivated successfully"));
    }
}


