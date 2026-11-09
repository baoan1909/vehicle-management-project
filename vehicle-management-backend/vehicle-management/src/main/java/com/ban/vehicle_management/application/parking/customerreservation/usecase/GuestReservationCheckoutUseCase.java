package com.ban.vehicle_management.application.parking.customerreservation.usecase;

import com.ban.vehicle_management.application.billing.payment.model.command.CreateVnpayPaymentCommand;
import com.ban.vehicle_management.application.billing.payment.port.in.VnpayPaymentPortIn;
import com.ban.vehicle_management.application.parking.customerreservation.model.ReservationCheckout;
import com.ban.vehicle_management.application.parking.customerreservation.port.in.CustomerReservationPortIn;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuestReservationCheckoutUseCase {
    private static final BigDecimal VNPAY_MINIMUM = BigDecimal.valueOf(10_000);

    private final CustomerReservationPortIn reservations;
    private final VnpayPaymentPortIn vnpay;

    public GuestReservationCheckoutUseCase(CustomerReservationPortIn reservations,
            VnpayPaymentPortIn vnpay) {
        this.reservations = reservations;
        this.vnpay = vnpay;
    }

    @Transactional
    public ReservationCheckout create(UUID parkingLotId, UUID zoneId, UUID parkingSpaceId,
            UUID vehicleTypeId, String licensePlate, boolean policyAccepted, String clientIp) {
        var reservation = reservations.createGuest(parkingLotId, zoneId, parkingSpaceId,
                vehicleTypeId, licensePlate, policyAccepted);
        if (reservation.prepaidAmount().compareTo(VNPAY_MINIMUM) < 0) {
            throw new ConflictException("Payment for reservations below 10,000 VND is temporarily unavailable");
        }
        var payment = vnpay.createGuestReservationPayment(reservation.reservationId(),
                reservation.accessToken(), new CreateVnpayPaymentCommand(null, "vn", clientIp));
        return new ReservationCheckout(reservation, payment.paymentUrl(), "VNPAY");
    }
}
