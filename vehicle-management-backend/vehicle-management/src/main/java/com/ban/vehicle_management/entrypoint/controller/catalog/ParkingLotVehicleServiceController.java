package com.ban.vehicle_management.entrypoint.controller.catalog;

import com.ban.vehicle_management.application.catalog.availability.mapper.ParkingLotCatalogAvailabilityApiMapper;
import com.ban.vehicle_management.application.catalog.availability.port.in.ParkingLotCatalogAvailabilityPortIn;
import com.ban.vehicle_management.entrypoint.dto.catalog.availability.request.UpdateParkingLotVehicleServiceRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.availability.response.ParkingLotCatalogAvailabilityResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog/parking-lots/availability")
public class ParkingLotVehicleServiceController {
    private final ParkingLotCatalogAvailabilityPortIn availabilityPortIn;
    private final ParkingLotCatalogAvailabilityApiMapper mapper;

    public ParkingLotVehicleServiceController(
            ParkingLotCatalogAvailabilityPortIn availabilityPortIn,
            ParkingLotCatalogAvailabilityApiMapper mapper
    ) {
        this.availabilityPortIn = availabilityPortIn;
        this.mapper = mapper;
    }

    @PutMapping("/vehicle-types/{vehicleTypeId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('VEHICLE_TYPE_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<List<ParkingLotCatalogAvailabilityResponse>>> updateVehicleType(
            @PathVariable UUID vehicleTypeId,
            @Valid @RequestBody UpdateParkingLotVehicleServiceRequest request
    ) {
        List<ParkingLotCatalogAvailabilityResponse> result = mapper.toResponses(
                availabilityPortIn.setVehicleTypeEnabled(
                        request.parkingLotIds(), vehicleTypeId, request.enabled()));
        return ResponseEntity.ok(ApiResponse.ok("Updated parking lot vehicle service availability", result));
    }
}
