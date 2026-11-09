package com.ban.vehicle_management.entrypoint.controller.parking;

import com.ban.vehicle_management.application.billing.payment.mapper.PaymentApiMapper;
import com.ban.vehicle_management.application.billing.payment.model.command.CreateVnpayPaymentCommand;
import com.ban.vehicle_management.application.billing.payment.port.in.VnpayPaymentPortIn;
import com.ban.vehicle_management.application.parking.customerreservation.mapper.CustomerReservationApiMapper;
import com.ban.vehicle_management.application.parking.customerreservation.port.in.CustomerReservationPortIn;
import com.ban.vehicle_management.application.parking.customerreservation.port.in.ParkingPlateLookupPortIn;
import com.ban.vehicle_management.application.parking.customerreservation.usecase.GuestReservationCheckoutUseCase;
import com.ban.vehicle_management.application.parking.parkinglayout.mapper.ParkingLayoutApiMapper;
import com.ban.vehicle_management.application.parking.parkinglayout.port.in.ParkingLayoutPortIn;
import com.ban.vehicle_management.entrypoint.dto.billing.payment.request.CreateVnpayPaymentRequest;
import com.ban.vehicle_management.entrypoint.dto.billing.payment.response.VnpayPaymentResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.customerreservation.request.CreateCustomerReservationRequest;
import com.ban.vehicle_management.entrypoint.dto.parking.customerreservation.response.CustomerReservationResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.customerreservation.response.ParkingPlateLookupResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.customerreservation.response.ReservationOptionResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.customerreservation.response.PublicReservationLayoutResponse;
import com.ban.vehicle_management.entrypoint.dto.parking.customerreservation.response.ReservationCheckoutResponse;
import com.ban.vehicle_management.entrypoint.converter.ClientIpResolver;
import com.ban.vehicle_management.shared.utils.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/parking/reservations")
public class PublicCustomerReservationController {
    private final CustomerReservationPortIn reservations;
    private final ParkingPlateLookupPortIn plateLookup;
    private final CustomerReservationApiMapper reservationMapper;
    private final VnpayPaymentPortIn vnpay;
    private final PaymentApiMapper paymentMapper;
    private final GuestReservationCheckoutUseCase checkout;
    private final ParkingLayoutPortIn layouts;
    private final ParkingLayoutApiMapper layoutMapper;
    private final ClientIpResolver clientIpResolver;

    public PublicCustomerReservationController(CustomerReservationPortIn reservations,
            ParkingPlateLookupPortIn plateLookup,
            CustomerReservationApiMapper reservationMapper, VnpayPaymentPortIn vnpay,
            PaymentApiMapper paymentMapper, GuestReservationCheckoutUseCase checkout,
            ParkingLayoutPortIn layouts, ParkingLayoutApiMapper layoutMapper,
            ClientIpResolver clientIpResolver) {
        this.reservations = reservations;
        this.plateLookup = plateLookup;
        this.reservationMapper = reservationMapper;
        this.vnpay = vnpay;
        this.paymentMapper = paymentMapper;
        this.checkout = checkout;
        this.layouts = layouts;
        this.layoutMapper = layoutMapper;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping("/lookup")
    public ResponseEntity<ApiResponse<ParkingPlateLookupResponse>> lookup(
            @RequestParam String licensePlate) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok(
                "Active parking records fetched", reservationMapper.toResponse(
                        plateLookup.lookup(licensePlate))));
    }

    @GetMapping("/options")
    public ResponseEntity<ApiResponse<List<ReservationOptionResponse>>> options(
            @RequestParam UUID parkingLotId, @RequestParam UUID vehicleTypeId) {
        return ResponseEntity.ok(ApiResponse.ok("Reservation options fetched",
                reservationMapper.toOptionResponses(reservations.findOptions(parkingLotId, vehicleTypeId))));
    }

    @GetMapping("/options/{zoneId}/layout")
    public ResponseEntity<ApiResponse<PublicReservationLayoutResponse>> layout(
            @PathVariable UUID zoneId, @RequestParam UUID parkingLotId) {
        return ResponseEntity.ok(ApiResponse.ok("Published reservation layout fetched",
                layoutMapper.toPublicReservationResponse(
                        layouts.getPublicPublishedLayout(parkingLotId, zoneId))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReservationCheckoutResponse>> create(
            @RequestBody CreateCustomerReservationRequest request, HttpServletRequest httpRequest) {
        var result = checkout.create(request.parkingLotId(), request.zoneId(),
                request.parkingSpaceId(), request.vehicleTypeId(), request.licensePlate(),
                request.policyAccepted(), clientIpResolver.resolve(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "Reservation awaiting payment", reservationMapper.toResponse(result)));
    }

    @GetMapping("/{reservationId}")
    public ResponseEntity<ApiResponse<CustomerReservationResponse>> get(
            @PathVariable UUID reservationId,
            @RequestHeader("X-Reservation-Access") String accessToken) {
        return ResponseEntity.ok(ApiResponse.ok("Reservation fetched",
                reservationMapper.toResponse(reservations.getGuest(reservationId, accessToken))));
    }

    @PostMapping("/{reservationId}/cancel")
    public ResponseEntity<ApiResponse<CustomerReservationResponse>> cancel(
            @PathVariable UUID reservationId,
            @RequestHeader("X-Reservation-Access") String accessToken) {
        return ResponseEntity.ok(ApiResponse.ok("Reservation cancelled without refund",
                reservationMapper.toResponse(reservations.cancelGuest(reservationId, accessToken))));
    }

    @PostMapping("/{reservationId}/payments/vnpay")
    public ResponseEntity<ApiResponse<VnpayPaymentResponse>> createVnpayPayment(
            @PathVariable UUID reservationId,
            @RequestHeader("X-Reservation-Access") String accessToken,
            @RequestBody(required = false) CreateVnpayPaymentRequest request,
            HttpServletRequest httpRequest) {
        var result = vnpay.createGuestReservationPayment(reservationId, accessToken,
                paymentMapper.toCommandWithDefaults(request, clientIpResolver.resolve(httpRequest)));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                "VNPAY payment created", paymentMapper.toResponse(result)));
    }

}
