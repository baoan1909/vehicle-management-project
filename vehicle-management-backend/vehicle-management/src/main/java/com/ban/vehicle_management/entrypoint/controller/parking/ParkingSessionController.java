package com.ban.vehicle_management.entrypoint.controller.parking;

import com.ban.vehicle_management.application.parking.parkingsession.mapper.ParkingSessionApiMapper;
import com.ban.vehicle_management.application.parking.parkingsession.port.in.ParkingSessionPortIn;
import com.ban.vehicle_management.application.parking.checkinspacehold.mapper.CheckInSpaceHoldApiMapper;
import com.ban.vehicle_management.application.parking.checkinspacehold.port.in.CheckInSpaceHoldPortIn;
import com.ban.vehicle_management.entrypoint.dto.parking.checkinspacehold.request.CreateCheckInSpaceHoldRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.checkinspacehold.response.CheckInSpaceHoldResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.request.CheckInParkingSessionRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.request.CheckInPreviewRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.request.CheckOutParkingSessionRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.request.CheckOutQrPreviewRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.request.ParkingSessionFilterRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.response.ParkingSessionCheckInResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.response.ParkingSessionCheckInPreviewResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.response.ParkingSessionCheckOutPreviewResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.response.ParkingSessionCheckOutResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.response.ParkingSessionManagementResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsession.response.ParkingSessionUserResponse;
import com.ban.vehicle_management.entrypoint.converter.ParkingSessionRequestConverter;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/parking/parking-sessions")
public class ParkingSessionController {

    private final ParkingSessionPortIn parkingSessionPortIn;
    private final ParkingSessionApiMapper parkingSessionApiMapper;
    private final ParkingSessionRequestConverter requestConverter;
    private final CheckInSpaceHoldPortIn checkInSpaceHoldPortIn;
    private final CheckInSpaceHoldApiMapper checkInSpaceHoldApiMapper;

    public ParkingSessionController(
            ParkingSessionPortIn parkingSessionPortIn,
            ParkingSessionApiMapper parkingSessionApiMapper,
            ParkingSessionRequestConverter requestConverter,
            CheckInSpaceHoldPortIn checkInSpaceHoldPortIn,
            CheckInSpaceHoldApiMapper checkInSpaceHoldApiMapper
    ) {
        this.parkingSessionPortIn = parkingSessionPortIn;
        this.parkingSessionApiMapper = parkingSessionApiMapper;
        this.requestConverter = requestConverter;
        this.checkInSpaceHoldPortIn = checkInSpaceHoldPortIn;
        this.checkInSpaceHoldApiMapper = checkInSpaceHoldApiMapper;
    }

    @PostMapping(value = "/check-in/space-holds", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SESSION_CHECK_IN_ALL')")
    public ResponseEntity<ApiResponse<CheckInSpaceHoldResponse>> createCheckInSpaceHold(
            @RequestBody CreateCheckInSpaceHoldRequest request
    ) {
        var result = checkInSpaceHoldPortIn.create(checkInSpaceHoldApiMapper.toCommand(request));
        return ResponseEntity.status(checkInSpaceHoldApiMapper.toCreateStatus(result)).body(ApiResponse.ok(
                checkInSpaceHoldApiMapper.toCreateMessage(result),
                checkInSpaceHoldApiMapper.toResponse(result)));
    }

    @DeleteMapping("/check-in/space-holds/{holdId}")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SESSION_CHECK_IN_ALL')")
    public ResponseEntity<ApiResponse<Void>> cancelCheckInSpaceHold(
            @PathVariable UUID holdId,
            @RequestParam(required = false) String reason
    ) {
        checkInSpaceHoldPortIn.cancel(holdId, reason);
        return ResponseEntity.ok(ApiResponse.ok("Check-in parking-space hold cancelled"));
    }

    @GetMapping
    @PreAuthorize("@permissionAuthorizer.hasAnyPermission('CARD_READ_ALL', 'PARKING_SESSION_CHECK_IN_ALL', 'PARKING_SESSION_CHECK_OUT_ALL')")
    public ResponseEntity<ApiResponse<List<ParkingSessionManagementResponse>>> getParkingSessions(
            @ModelAttribute ParkingSessionFilterRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched parking sessions successfully",
                parkingSessionApiMapper.toManagementResponses(parkingSessionPortIn.getSessions(
                        request.status(),
                        request.vehicleTypeId(),
                        request.zoneId(),
                        request.fromDate(),
                        request.toDate(),
                        request.keyword(),
                        request.parkingLotId()
                ))
        ));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ParkingSessionUserResponse>>> getMyParkingSessions(
            @ModelAttribute ParkingSessionFilterRequest request,
            @RequestParam(required = false) Integer limit
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Fetched customer parking sessions successfully",
                parkingSessionApiMapper.toUserResponses(parkingSessionPortIn.getOwnSessions(
                        request.status(),
                        request.vehicleTypeId(),
                        request.zoneId(),
                        request.fromDate(),
                        request.toDate(),
                        request.keyword(),
                        request.parkingLotId(),
                        limit
                ))
        ));
    }

    @PostMapping(value = "/check-in", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SESSION_CHECK_IN_ALL')")
    public ResponseEntity<ApiResponse<ParkingSessionCheckInResponse>> checkIn(
            @RequestPart("request") String request,
            @RequestPart("licensePlateImage") MultipartFile licensePlateImage,
            @RequestPart("vehicleImage") MultipartFile vehicleImage,
            @RequestPart("personImage") MultipartFile personImage
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Parking session checked in successfully",
                parkingSessionApiMapper.toCheckInResponse(parkingSessionPortIn.checkIn(
                        parkingSessionApiMapper.toCommand(
                                requestConverter.toCheckInRequest(request), licensePlateImage, vehicleImage, personImage)))));
    }

    @PostMapping(value = "/check-in/preview", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SESSION_CHECK_IN_ALL')")
    public ResponseEntity<ApiResponse<ParkingSessionCheckInPreviewResponse>> previewCheckIn(
            @RequestBody CheckInPreviewRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Check-in preview calculated successfully",
                parkingSessionApiMapper.toCheckInPreviewResponse(
                        parkingSessionPortIn.previewCheckIn(parkingSessionApiMapper.toCommand(request)))));
    }

    @PostMapping(value = "/check-out", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SESSION_CHECK_OUT_ALL')")
    public ResponseEntity<ApiResponse<ParkingSessionCheckOutResponse>> checkOut(
            @RequestPart("request") String request,
            @RequestPart("licensePlateImage") MultipartFile licensePlateImage,
            @RequestPart("vehicleImage") MultipartFile vehicleImage,
            @RequestPart("personImage") MultipartFile personImage
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Parking session checked out successfully",
                parkingSessionApiMapper.toCheckOutResponse(parkingSessionPortIn.checkOut(
                        parkingSessionApiMapper.toCommand(
                                requestConverter.toCheckOutRequest(request), licensePlateImage, vehicleImage, personImage)))));
    }

    @PostMapping(value = "/check-out/prepare", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SESSION_CHECK_OUT_ALL')")
    public ResponseEntity<ApiResponse<ParkingSessionCheckOutResponse>> prepareVisitorCheckOut(
            @RequestPart("request") String request,
            @RequestPart("licensePlateImage") MultipartFile licensePlateImage,
            @RequestPart("vehicleImage") MultipartFile vehicleImage,
            @RequestPart("personImage") MultipartFile personImage
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Visitor checkout prepared successfully",
                parkingSessionApiMapper.toCheckOutResponse(parkingSessionPortIn.prepareVisitorCheckOut(
                        parkingSessionApiMapper.toCommand(
                                requestConverter.toCheckOutRequest(request),
                                licensePlateImage,
                                vehicleImage,
                                personImage
                        )
                ))
        ));
    }

    @GetMapping("/check-out/by-invoice")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SESSION_CHECK_OUT_ALL')")
    public ResponseEntity<ApiResponse<ParkingSessionCheckOutResponse>> getCheckOutByInvoice(
            @RequestParam UUID invoiceId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Checkout payment state fetched successfully",
                parkingSessionApiMapper.toCheckOutResponse(
                        parkingSessionPortIn.getCheckOutByInvoice(invoiceId)
                )
        ));
    }

    @GetMapping("/open-by-card")
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SESSION_CHECK_OUT_ALL')")
    public ResponseEntity<ApiResponse<ParkingSessionCheckOutPreviewResponse>> findOpenByCardUid(@RequestParam String cardUid) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Open parking session preview fetched successfully",
                parkingSessionApiMapper.toCheckOutPreviewResponse(parkingSessionPortIn.previewCheckOutByCardUid(cardUid))
        ));
    }

    @PostMapping(value = "/open-by-subscription-qr", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@permissionAuthorizer.hasPermission('PARKING_SESSION_CHECK_OUT_ALL')")
    public ResponseEntity<ApiResponse<ParkingSessionCheckOutPreviewResponse>> findOpenBySubscriptionQr(
            @RequestBody CheckOutQrPreviewRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Open subscription parking session preview fetched successfully",
                parkingSessionApiMapper.toCheckOutPreviewResponse(
                        parkingSessionPortIn.previewCheckOutBySubscriptionQr(
                                request.subscriptionQrPayload(),
                                request.parkingLotId()
                        )
                )
        ));
    }

}
