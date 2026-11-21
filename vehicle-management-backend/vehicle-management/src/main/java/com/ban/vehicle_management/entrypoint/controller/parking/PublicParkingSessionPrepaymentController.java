package com.ban.vehicle_management.entrypoint.controller.parking;

import com.ban.vehicle_management.application.billing.payment.port.in.SepayPaymentPortIn;
import com.ban.vehicle_management.application.parking.parkingsessionprepayment.mapper.ParkingSessionPrepaymentApiMapper;
import com.ban.vehicle_management.application.parking.parkingsessionprepayment.port.in.ParkingSessionPrepaymentPortIn;
import com.ban.vehicle_management.application.parking.parkingsessionprepayment.usecase.GuestParkingSessionPrepaymentCheckoutUseCase;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsessionprepayment.request.CreateParkingSessionPrepaymentRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsessionprepayment.request.ParkingSessionPrepaymentAccessRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsessionprepayment.response.ParkingSessionPrepaymentCheckoutResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.parkingsessionprepayment.response.ParkingSessionPrepaymentResponse;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/parking/prepayments")
public class PublicParkingSessionPrepaymentController {
    private final ParkingSessionPrepaymentPortIn prepayments;
    private final GuestParkingSessionPrepaymentCheckoutUseCase checkout;
    private final ParkingSessionPrepaymentApiMapper mapper;
    private final SepayPaymentPortIn sepay;

    public PublicParkingSessionPrepaymentController(
            ParkingSessionPrepaymentPortIn prepayments,
            GuestParkingSessionPrepaymentCheckoutUseCase checkout,
            ParkingSessionPrepaymentApiMapper mapper,
            SepayPaymentPortIn sepay
    ) {
        this.prepayments = prepayments;
        this.checkout = checkout;
        this.mapper = mapper;
        this.sepay = sepay;
    }

    @PostMapping("/lookup")
    public ResponseEntity<ApiResponse<ParkingSessionPrepaymentResponse>> lookup(
            @Valid @RequestBody ParkingSessionPrepaymentAccessRequest request
    ) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok(
                "Open parking session fetched",
                mapper.toResponse(prepayments.lookup(request.licensePlate(), request.cardNumber()))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ParkingSessionPrepaymentCheckoutResponse>> create(
            @Valid @RequestBody CreateParkingSessionPrepaymentRequest request
    ) {
        var result = checkout.create(request.licensePlate(), request.cardNumber(),
                request.estimatedMinutes(), request.policyAccepted());
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok("Parking prepayment awaiting bank transfer",
                        mapper.toResponse(result)));
    }

    @PostMapping("/{prepaymentId}/status")
    public ResponseEntity<ApiResponse<ParkingSessionPrepaymentResponse>> status(
            @PathVariable UUID prepaymentId,
            @Valid @RequestBody ParkingSessionPrepaymentAccessRequest request
    ) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok(
                "Parking prepayment status fetched",
                mapper.toResponse(prepayments.get(
                        prepaymentId, request.licensePlate(), request.cardNumber()))));
    }

    @PostMapping("/{prepaymentId}/qr")
    public ResponseEntity<ApiResponse<byte[]>> downloadQr(
            @PathVariable UUID prepaymentId,
            @Valid @RequestBody ParkingSessionPrepaymentAccessRequest request
    ) {
        var image = sepay.downloadParkingSessionPrepaymentQr(prepaymentId, request.licensePlate(), request.cardNumber());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok("SePay payment QR fetched", image));
    }
}
