package com.ban.vehicle_management.application.parking.parkingsessionprepayment.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.accesscontrol.card.port.out.CardPortOut;
import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.service.ParkingSessionChargeService;
import com.ban.vehicle_management.application.parking.parkingsessionprepayment.port.out.ParkingSessionPrepaymentPortOut;
import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.domain.parking.parkingsessionprepayment.model.ParkingSessionPrepayment;
import com.ban.vehicle_management.shared.enumeration.billing.InvoicePurpose;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParkingSessionPrepaymentUseCaseImplTest {
    @Mock private CardPortOut cards;
    @Mock private ParkingSessionPortOut sessions;
    @Mock private ParkingSessionPrepaymentPortOut prepayments;
    @Mock private ParkingSessionChargeService charges;
    @Mock private InvoicePortOut invoices;
    @Mock private ParkingLotPortOut parkingLots;

    private ParkingSessionPrepaymentUseCaseImpl useCase;
    private Card card;
    private ParkingSession session;

    @BeforeEach
    void setUp() {
        useCase = new ParkingSessionPrepaymentUseCaseImpl(
                cards, sessions, prepayments, charges, invoices, parkingLots);
        card = new Card();
        card.setCardId(UUID.randomUUID());
        card.setCardNumber("VIS-0001");
        session = new ParkingSession();
        session.setParkingSessionId(UUID.randomUUID());
        session.setCardId(card.getCardId());
        session.setParkingLotId(UUID.randomUUID());
        session.setLicensePlateIn("60A99999");
        session.setCheckInTime(Instant.parse("2026-11-20T01:00:00Z"));
    }

    @Test
    void createsQuoteForOnlyTheUnpaidProjectedDifference() {
        when(cards.findByCardNumberForUpdate("VIS-0001")).thenReturn(Optional.of(card));
        when(sessions.findOpenByCardIdForUpdate(card.getCardId())).thenReturn(Optional.of(session));
        when(invoices.existsByParkingSessionIdAndPurposeAndStatusIn(
                eq(session.getParkingSessionId()), eq(InvoicePurpose.PARKING_SESSION), anyCollection()))
                .thenReturn(false);
        when(charges.grossAmount(any(), any())).thenReturn(new BigDecimal("30000"));
        when(charges.successfulPrepaidAmount(session)).thenReturn(new BigDecimal("8000"));
        when(invoices.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(prepayments.save(any(ParkingSessionPrepayment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = useCase.create("60A-999.99", "VIS-0001", 30, true);

        assertEquals(new BigDecimal("22000"), result.amount());
        assertEquals(new BigDecimal("30000"), result.projectedGrossAmount());
        assertEquals(new BigDecimal("8000"), result.paidBeforeAmount());
        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoices).save(invoiceCaptor.capture());
        assertEquals(InvoicePurpose.PARKING_SESSION_PREPAYMENT,
                invoiceCaptor.getValue().getPurpose());
        assertEquals(new BigDecimal("22000"), invoiceCaptor.getValue().getFinalAmount());
    }

    @Test
    void hidesSessionWhenLicensePlateDoesNotMatchCard() {
        when(cards.findByCardNumber("VIS-0001")).thenReturn(Optional.of(card));
        when(sessions.findOpenByCardId(card.getCardId())).thenReturn(Optional.of(session));

        assertThrows(NotFoundException.class,
                () -> useCase.lookup("51A-111.11", "VIS-0001"));
        verify(charges, never()).successfulPrepaidAmount(any());
    }
}
