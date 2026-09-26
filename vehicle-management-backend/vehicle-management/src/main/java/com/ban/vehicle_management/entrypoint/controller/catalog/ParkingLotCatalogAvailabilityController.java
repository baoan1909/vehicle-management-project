package com.ban.vehicle_management.entrypoint.controller.catalog;

import com.ban.vehicle_management.application.catalog.availability.mapper.ParkingLotCatalogAvailabilityApiMapper;
import com.ban.vehicle_management.application.catalog.availability.port.in.ParkingLotCatalogAvailabilityPortIn;
import com.ban.vehicle_management.entrypoint.dto.catalog.availability.request.UpdateParkingLotCatalogAvailabilityRequest;
import com.ban.vehicle_management.entrypoint.dto.catalog.availability.response.ParkingLotCatalogAvailabilityResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog/parking-lots/{parkingLotId}/availability")
public class ParkingLotCatalogAvailabilityController {
    private final ParkingLotCatalogAvailabilityPortIn availabilityPortIn;
    private final ParkingLotCatalogAvailabilityApiMapper mapper;

    public ParkingLotCatalogAvailabilityController(ParkingLotCatalogAvailabilityPortIn availabilityPortIn,
                                                   ParkingLotCatalogAvailabilityApiMapper mapper) {
        this.availabilityPortIn = availabilityPortIn;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_LOT_READ_ALL')")
    public ResponseEntity<ApiResponse<ParkingLotCatalogAvailabilityResponse>> get(@PathVariable UUID parkingLotId) {
        return ResponseEntity.ok(ApiResponse.ok("Fetched parking lot catalog availability",
                mapper.toResponse(availabilityPortIn.getAvailability(parkingLotId))));
    }

    @PutMapping("/vehicle-types/{vehicleTypeId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('VEHICLE_TYPE_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<ParkingLotCatalogAvailabilityResponse>> updateVehicleType(
            @PathVariable UUID parkingLotId, @PathVariable UUID vehicleTypeId,
            @RequestBody UpdateParkingLotCatalogAvailabilityRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Updated parking lot vehicle type availability",
                mapper.toResponse(availabilityPortIn.setVehicleTypeEnabled(parkingLotId, vehicleTypeId, request.enabled()))));
    }

    @PutMapping("/ticket-types/{ticketTypeId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('TICKET_TYPE_UPDATE_ALL')")
    public ResponseEntity<ApiResponse<ParkingLotCatalogAvailabilityResponse>> updateTicketType(
            @PathVariable UUID parkingLotId, @PathVariable UUID ticketTypeId,
            @RequestBody UpdateParkingLotCatalogAvailabilityRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Updated parking lot ticket type availability",
                mapper.toResponse(availabilityPortIn.setTicketTypeEnabled(parkingLotId, ticketTypeId, request.enabled()))));
    }
}
