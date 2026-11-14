package com.ban.vehicle_management.application.parking.customerreservation.usecase;

import com.ban.vehicle_management.application.billing.payment.model.command.CreateVnpayPaymentCommand;
import com.ban.vehicle_management.application.billing.payment.port.in.SepayPaymentPortIn;
import com.ban.vehicle_management.application.billing.payment.port.in.VnpayPaymentPortIn;
import com.ban.vehicle_management.application.parking.customerreservation.model.ReservationCheckout;
import com.ban.vehicle_management.application.parking.customerreservation.port.in.CustomerReservationPortIn;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuestReservationCheckoutUseCase {
    private final CustomerReservationPortIn reservations;
    private final VnpayPaymentPortIn vnpay;
    private final SepayPaymentPortIn sepay;
    private final BigDecimal vnpayMinimumAmount;

    public GuestReservationCheckoutUseCase(CustomerReservationPortIn reservations,
            VnpayPaymentPortIn vnpay, SepayPaymentPortIn sepay,
            @Value("${app.payment.vnpay.minimum-amount:10000}") BigDecimal vnpayMinimumAmount) {
        this.reservations = reservations;
        this.vnpay = vnpay;
        this.sepay = sepay;
        this.vnpayMinimumAmount = vnpayMinimumAmount;
    }

    @Transactional
    public ReservationCheckout create(UUID parkingLotId, UUID zoneId, UUID parkingSpaceId,
            UUID vehicleTypeId, String licensePlate, boolean policyAccepted, String clientIp) {
        var reservation = reservations.createGuest(parkingLotId, zoneId, parkingSpaceId,
                vehicleTypeId, licensePlate, policyAccepted);
        if (reservation.prepaidAmount().compareTo(vnpayMinimumAmount) < 0) {
            var payment = sepay.createGuestReservationPayment(reservation.reservationId(),
                    reservation.accessToken());
            return new ReservationCheckout(
                    reservation, payment.qrCodeUrl(), "SEPAY", payment.transactionRef(),
                    payment.bankCode(), payment.accountNumber(), payment.accountHolder(),
                    payment.expiresAt());
        }
        var payment = vnpay.createGuestReservationPayment(reservation.reservationId(),
                reservation.accessToken(), new CreateVnpayPaymentCommand(null, "vn", clientIp));
        return new ReservationCheckout(
                reservation, payment.paymentUrl(), "VNPAY", payment.transactionRef(),
                null, null, null, payment.expiresAt());
    }
}
