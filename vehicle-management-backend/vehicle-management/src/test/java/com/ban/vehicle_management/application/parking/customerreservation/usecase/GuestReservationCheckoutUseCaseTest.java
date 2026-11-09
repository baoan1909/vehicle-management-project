package com.ban.vehicle_management.application.parking.customerreservation.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.billing.payment.model.command.CreateVnpayPaymentCommand;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayPaymentResult;
import com.ban.vehicle_management.application.billing.payment.port.in.VnpayPaymentPortIn;
import com.ban.vehicle_management.application.parking.customerreservation.model.ReservationView;
import com.ban.vehicle_management.application.parking.customerreservation.port.in.CustomerReservationPortIn;
import com.ban.vehicle_management.shared.enumeration.parking.CustomerReservationStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GuestReservationCheckoutUseCaseTest {
    @Mock private CustomerReservationPortIn reservations;
    @Mock private VnpayPaymentPortIn vnpay;
    @InjectMocks private GuestReservationCheckoutUseCase useCase;

    @Test
    void amountBelowVnpayMinimumIsRejectedWithoutStartingPayment() {
        UUID lotId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();
        ReservationView reservation = reservation(lotId, zoneId, spaceId, vehicleTypeId, 5_000);
        when(reservations.createGuest(lotId, zoneId, spaceId, vehicleTypeId, "59A-12345", true))
                .thenReturn(reservation);
        assertThrows(ConflictException.class, () -> useCase.create(lotId, zoneId,
                spaceId, vehicleTypeId, "59A-12345", true, "127.0.0.1"));
        verifyNoInteractions(vnpay);
    }

    @Test
    void successfulPaymentLinkReturnsReservationAndRedirectTogether() {
        UUID lotId = UUID.randomUUID();
        UUID zoneId = UUID.randomUUID();
        UUID spaceId = UUID.randomUUID();
        UUID vehicleTypeId = UUID.randomUUID();
        ReservationView reservation = reservation(lotId, zoneId, spaceId, vehicleTypeId, 10_000);
        when(reservations.createGuest(lotId, zoneId, spaceId, vehicleTypeId, "59A-12345", true))
                .thenReturn(reservation);
        when(vnpay.createGuestReservationPayment(eq(reservation.reservationId()), eq("secret"),
                any(CreateVnpayPaymentCommand.class)))
                .thenReturn(new VnpayPaymentResult(UUID.randomUUID(), reservation.invoiceId(),
                        "VNP123", "https://sandbox.vnpayment.vn/example", Instant.now()));

        var result = useCase.create(lotId, zoneId, spaceId, vehicleTypeId,
                "59A-12345", true, "127.0.0.1");

        assertEquals(reservation, result.reservation());
        assertEquals("https://sandbox.vnpayment.vn/example", result.paymentUrl());
        assertEquals("VNPAY", result.paymentMethod());
    }

    private ReservationView reservation(UUID lotId, UUID zoneId, UUID spaceId,
            UUID vehicleTypeId, long amount) {
        return new ReservationView(UUID.randomUUID(), lotId, zoneId, spaceId, vehicleTypeId,
                "59A-12345", CustomerReservationStatus.PENDING_PAYMENT, UUID.randomUUID(),
                BigDecimal.valueOf(amount), Instant.now().plusSeconds(600), null, null,
                null, null, "secret", null, null, null, null);
    }
}
