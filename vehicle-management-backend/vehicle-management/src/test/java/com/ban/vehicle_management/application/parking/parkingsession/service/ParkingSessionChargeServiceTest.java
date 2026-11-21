package com.ban.vehicle_management.application.parking.parkingsession.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.application.catalog.pricerule.port.out.PriceRulePortOut;
import com.ban.vehicle_management.application.parking.customerreservation.port.out.CustomerReservationPortOut;
import com.ban.vehicle_management.domain.parking.customerreservation.model.CustomerReservation;
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.shared.enumeration.billing.InvoicePurpose;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParkingSessionChargeServiceTest {
    @Mock private PriceRulePortOut priceRules;
    @Mock private CustomerReservationPortOut reservations;
    @Mock private InvoicePortOut invoices;

    private ParkingSessionChargeService service;

    @BeforeEach
    void setUp() {
        service = new ParkingSessionChargeService(priceRules, reservations, invoices);
    }

    @Test
    void subtractsReservationAndSuccessfulSessionPrepaymentsOnce() {
        UUID sessionId = UUID.randomUUID();
        ParkingSession session = new ParkingSession();
        session.setParkingSessionId(sessionId);

        CustomerReservation reservation = new CustomerReservation();
        reservation.setParkingSessionId(sessionId);
        reservation.setPaidAt(Instant.parse("2026-11-20T01:00:00Z"));
        reservation.setPrepaidAmount(new BigDecimal("30000"));
        reservation.setDayPrice(new BigDecimal("30000"));
        reservation.setNightPrice(new BigDecimal("50000"));
        reservation.setDayStart(LocalTime.of(6, 0));
        reservation.setDayEnd(LocalTime.of(18, 0));

        when(reservations.findBySessionId(sessionId)).thenReturn(Optional.of(reservation));
        when(invoices.sumFinalAmountByParkingSessionIdAndPurposeAndStatus(
                sessionId, InvoicePurpose.PARKING_SESSION_PREPAYMENT, InvoiceStatus.PAID))
                .thenReturn(new BigDecimal("10000"));

        Instant projectedCheckOut = Instant.parse("2026-11-20T04:00:00Z");

        assertEquals(new BigDecimal("30000"), service.grossAmount(session, projectedCheckOut));
        assertEquals(new BigDecimal("40000"), service.successfulPrepaidAmount(session));
        assertEquals(BigDecimal.ZERO, service.amountDue(session, projectedCheckOut));
    }
}
