package com.ban.vehicle_management.entrypoint.controller.parking;

import com.ban.vehicle_management.application.parking.customerreservation.mapper.CustomerReservationApiMapper;
import com.ban.vehicle_management.application.parking.customerreservation.port.in.CustomerReservationPortIn;
import com.ban.vehicle_management.entrypoint.dto.parking.customerreservation.request.CreateCustomerReservationRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.customerreservation.response.CustomerReservationResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.customerreservation.response.ReservationOptionResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parking/reservations")
public class CustomerReservationController {
    private final CustomerReservationPortIn reservations;
    private final CustomerReservationApiMapper mapper;

    public CustomerReservationController(CustomerReservationPortIn reservations,
            CustomerReservationApiMapper mapper) {
        this.reservations = reservations;
        this.mapper = mapper;
    }

    @GetMapping("/options")
    public ResponseEntity<ApiResponse<List<ReservationOptionResponse>>> options(
            @RequestParam UUID parkingLotId, @RequestParam UUID vehicleTypeId) {
        return ResponseEntity.ok(ApiResponse.ok("Reservation options fetched",
                mapper.toOptionResponses(reservations.findOptions(parkingLotId, vehicleTypeId))));
    }

    @PostMapping
    @PreAuthorize("@permissionAuthorizer.hasPermission('PAYMENT_CREATE_OWN')")
    public ResponseEntity<ApiResponse<CustomerReservationResponse>> create(
            @RequestBody CreateCustomerReservationRequest request) {
        var value = reservations.create(request.parkingLotId(), request.zoneId(),
                request.parkingSpaceId(), request.vehicleTypeId(), request.licensePlate(),
                request.policyAccepted());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Reservation awaiting payment", mapper.toResponse(value)));
    }

    @GetMapping("/mine")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PAYMENT_CREATE_OWN')")
    public ResponseEntity<ApiResponse<List<CustomerReservationResponse>>> mine() {
        return ResponseEntity.ok(ApiResponse.ok("Reservations fetched",
                mapper.toResponses(reservations.findMine())));
    }

    @GetMapping("/{reservationId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PAYMENT_CREATE_OWN')")
    public ResponseEntity<ApiResponse<CustomerReservationResponse>> get(@PathVariable UUID reservationId) {
        return ResponseEntity.ok(ApiResponse.ok("Reservation fetched",
                mapper.toResponse(reservations.getMine(reservationId))));
    }

    @PostMapping("/{reservationId}/cancel")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PAYMENT_CREATE_OWN')")
    public ResponseEntity<ApiResponse<CustomerReservationResponse>> cancel(@PathVariable UUID reservationId) {
        return ResponseEntity.ok(ApiResponse.ok("Reservation cancelled without refund",
                mapper.toResponse(reservations.cancel(reservationId))));
    }
}
