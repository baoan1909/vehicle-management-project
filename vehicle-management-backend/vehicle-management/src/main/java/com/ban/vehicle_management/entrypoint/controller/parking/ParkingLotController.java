package com.ban.vehicle_management.entrypoint.controller.parking;

import com.ban.vehicle_management.application.parking.parkinglot.mapper.ParkingLotApiMapper;
import com.ban.vehicle_management.application.parking.parkinglot.port.in.ParkingLotPortIn;
import com.ban.vehicle_management.domain.parking.parkinglot.model.ParkingLot;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglot.request.CreateParkingLotRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglot.request.ParkingLotFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglot.request.UpdateParkingLotRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkinglot.response.ParkingLotAdminResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/parking/parking-lots")
public class ParkingLotController {

    private final ParkingLotPortIn parkingLotPortIn;
    private final ParkingLotApiMapper parkingLotApiMapper;

    public ParkingLotController(
            ParkingLotPortIn parkingLotPortIn,
            ParkingLotApiMapper parkingLotApiMapper
    ) {
        this.parkingLotPortIn = parkingLotPortIn;
        this.parkingLotApiMapper = parkingLotApiMapper;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ParkingLotAdminResponse>> createParkingLot(
            @RequestBody CreateParkingLotRequest request
    ) {
        ParkingLot createdParkingLot = parkingLotPortIn.createParkingLot(
                parkingLotApiMapper.toDomain(request)
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Parking lot created successfully",
                parkingLotApiMapper.toAdminResponse(createdParkingLot)
        ));
    }

    @GetMapping("/{parkingLotId}")
    public ResponseEntity<ApiResponse<ParkingLotAdminResponse>> getParkingLotById(
            @PathVariable UUID parkingLotId
    ) {
        ParkingLot parkingLot = parkingLotPortIn.getParkingLotById(parkingLotId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched parking lot successfully",
                parkingLotApiMapper.toAdminResponse(parkingLot)
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ParkingLotAdminResponse>>> getParkingLots(
            @ModelAttribute ParkingLotFilterRequest request
    ) {
        List<ParkingLot> parkingLots = parkingLotPortIn.getParkingLots(
                request.status(),
                request.keyword()
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched parking lots successfully",
                parkingLotApiMapper.toAdminResponses(parkingLots)
        ));
    }

    @PutMapping("/{parkingLotId}")
    public ResponseEntity<ApiResponse<ParkingLotAdminResponse>> updateParkingLot(
            @PathVariable UUID parkingLotId,
            @RequestBody UpdateParkingLotRequest request
    ) {
        ParkingLot updatedParkingLot = parkingLotPortIn.updateParkingLot(
                parkingLotId,
                parkingLotApiMapper.toDomain(request)
        );

        return ResponseEntity.ok(ApiResponse.ok(
                "Parking lot updated successfully",
                parkingLotApiMapper.toAdminResponse(updatedParkingLot)
        ));
    }

    @DeleteMapping("/{parkingLotId}")
    public ResponseEntity<ApiResponse<Void>> deleteParkingLot(
            @PathVariable UUID parkingLotId
    ) {
        parkingLotPortIn.deleteParkingLot(parkingLotId);

        return ResponseEntity.ok(ApiResponse.ok("Parking lot closed successfully"));
    }

    @PatchMapping("/{parkingLotId}/activate")
    public ResponseEntity<ApiResponse<ParkingLotAdminResponse>> activateParkingLot(
            @PathVariable UUID parkingLotId
    ) {
        ParkingLot activatedParkingLot = parkingLotPortIn.activateParkingLot(parkingLotId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Parking lot activated successfully",
                parkingLotApiMapper.toAdminResponse(activatedParkingLot)
        ));
    }

    @PatchMapping("/{parkingLotId}/maintenance")
    public ResponseEntity<ApiResponse<ParkingLotAdminResponse>> markParkingLotMaintenance(
            @PathVariable UUID parkingLotId
    ) {
        ParkingLot parkingLot = parkingLotPortIn.markParkingLotMaintenance(parkingLotId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Parking lot marked as maintenance successfully",
                parkingLotApiMapper.toAdminResponse(parkingLot)
        ));
    }

    @PatchMapping("/{parkingLotId}/close")
    public ResponseEntity<ApiResponse<ParkingLotAdminResponse>> closeParkingLot(
            @PathVariable UUID parkingLotId
    ) {
        ParkingLot closedParkingLot = parkingLotPortIn.closeParkingLot(parkingLotId);

        return ResponseEntity.ok(ApiResponse.ok(
                "Parking lot closed successfully",
                parkingLotApiMapper.toAdminResponse(closedParkingLot)
        ));
    }
}