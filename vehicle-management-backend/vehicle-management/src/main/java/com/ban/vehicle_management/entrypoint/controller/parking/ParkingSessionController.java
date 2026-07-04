package com.ban.vehicle_management.entrypoint.controller.parking;

import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.application.parking.parkingsession.mapper.ParkingSessionApiMapper;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.CheckInResult;
import com.ban.vehicle_management.application.parking.parkingsession.model.result.CheckOutResult;
import com.ban.vehicle_management.application.parking.parkingsession.port.in.ParkingSessionPortIn;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.request.CheckInParkingSessionRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.request.CheckOutParkingSessionRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.response.ParkingSessionCheckInResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.response.ParkingSessionCheckOutResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/parking/parking-sessions")
public class ParkingSessionController {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ParkingSessionPortIn parkingSessionPortIn;
    private final ParkingSessionApiMapper parkingSessionApiMapper;

    public ParkingSessionController(
            ParkingSessionPortIn parkingSessionPortIn,
            ParkingSessionApiMapper parkingSessionApiMapper
    ) {
        this.parkingSessionPortIn = parkingSessionPortIn;
        this.parkingSessionApiMapper = parkingSessionApiMapper;
    }

    @PostMapping(value = "/check-in", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SESSION_CHECK_IN_ALL')")
    public ResponseEntity<ApiResponse<ParkingSessionCheckInResponse>> checkIn(
            @RequestPart("request") String request,
            @RequestPart("licensePlateImage") MultipartFile licensePlateImage,
            @RequestPart("personImage") MultipartFile personImage
    ) {
        return buildCheckInResponse(parkingSessionPortIn.checkIn(
                parkingSessionApiMapper.toCommand(parseCheckInRequest(request), licensePlateImage, personImage)
        ));
    }

    @PostMapping(value = "/check-out", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SESSION_CHECK_OUT_ALL')")
    public ResponseEntity<ApiResponse<ParkingSessionCheckOutResponse>> checkOut(
            @RequestPart("request") String request,
            @RequestPart("licensePlateImage") MultipartFile licensePlateImage,
            @RequestPart("personImage") MultipartFile personImage
    ) {
        return buildCheckOutResponse(parkingSessionPortIn.checkOut(
                parkingSessionApiMapper.toCommand(parseCheckOutRequest(request), licensePlateImage, personImage)
        ));
    }

    private CheckInParkingSessionRequest parseCheckInRequest(String request) {
        try {
            return OBJECT_MAPPER.readValue(request, CheckInParkingSessionRequest.class);
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("request part must be valid JSON");
        }
    }

    private CheckOutParkingSessionRequest parseCheckOutRequest(String request) {
        try {
            return OBJECT_MAPPER.readValue(request, CheckOutParkingSessionRequest.class);
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("request part must be valid JSON");
        }
    }

    private ResponseEntity<ApiResponse<ParkingSessionCheckInResponse>> buildCheckInResponse(CheckInResult result) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Parking session checked in successfully",
                parkingSessionApiMapper.toCheckInResponse(result)
        ));
    }

    private ResponseEntity<ApiResponse<ParkingSessionCheckOutResponse>> buildCheckOutResponse(CheckOutResult result) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Parking session checked out successfully",
                parkingSessionApiMapper.toCheckOutResponse(result)
        ));
    }
}
