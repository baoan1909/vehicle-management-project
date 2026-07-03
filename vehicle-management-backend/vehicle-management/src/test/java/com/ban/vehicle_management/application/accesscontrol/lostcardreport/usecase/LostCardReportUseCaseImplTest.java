package com.ban.vehicle_management.application.accesscontrol.lostcardreport.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.accesscontrol.card.port.out.CardPortOut;
import com.ban.vehicle_management.application.accesscontrol.lostcardreport.authorization.LostCardReportAccessGuard;
import com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result.LostCardPreviewResult;
import com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result.LostCardReportWorkflowResult;
import com.ban.vehicle_management.application.accesscontrol.lostcardreport.port.out.LostCardReportPortOut;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionPortOut;
import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.application.billing.payment.port.out.PaymentPortOut;
import com.ban.vehicle_management.application.catalog.pricerule.port.out.PriceRulePortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.people.customervehicle.port.out.CustomerVehiclePortOut;
import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.domain.accesscontrol.lostcardreport.model.LostCardReport;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.catalog.pricerule.model.PriceRule;
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportContext;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportStatus;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;
import com.ban.vehicle_management.shared.enumeration.catalog.PriceRuleUnit;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LostCardReportUseCaseImplTest {

    private static final UUID REPORT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID PARKING_SESSION_ID = UUID.fromString("10000000-0000-0000-0000-000000000002");
    private static final UUID CARD_ID = UUID.fromString("10000000-0000-0000-0000-000000000003");
    private static final UUID NEW_CARD_ID = UUID.fromString("10000000-0000-0000-0000-000000000004");
    private static final UUID CUSTOMER_ID = UUID.fromString("10000000-0000-0000-0000-000000000005");
    private static final UUID CUSTOMER_VEHICLE_ID = UUID.fromString("10000000-0000-0000-0000-000000000006");
    private static final UUID VEHICLE_TYPE_ID = UUID.fromString("40000000-0000-0000-0000-000000000002");
    private static final UUID SUBSCRIPTION_ID = UUID.fromString("10000000-0000-0000-0000-000000000007");
    private static final UUID PRICE_RULE_ID = UUID.fromString("10000000-0000-0000-0000-000000000008");
    private static final UUID INVOICE_ID = UUID.fromString("10000000-0000-0000-0000-000000000009");
    private static final UUID ACCOUNT_ID = UUID.fromString("10000000-0000-0000-0000-000000000010");

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private LostCardReportAccessGuard lostCardReportAccessGuard;

    @Mock
    private LostCardReportPortOut lostCardReportPortOut;

    @Mock
    private ParkingSessionPortOut parkingSessionPortOut;

    @Mock
    private SubscriptionPortOut subscriptionPortOut;

    @Mock
    private CardPortOut cardPortOut;

    @Mock
    private PriceRulePortOut priceRulePortOut;

    @Mock
    private InvoicePortOut invoicePortOut;

    @Mock
    private PaymentPortOut paymentPortOut;

    @Mock
    private CustomerVehiclePortOut customerVehiclePortOut;

    @InjectMocks
    private LostCardReportUseCaseImpl lostCardReportUseCase;

    @Test
    void shouldPreviewVisitorLostCardInParking() {
        ParkingSession session = visitorOpenSession();
        stubVisitorPriceRules();

        when(parkingSessionPortOut.findOpenByLicensePlateIn("60K8-2301")).thenReturn(List.of(session));

        LostCardPreviewResult result = lostCardReportUseCase.previewByLicensePlate(" 60K8-2301 ");

        assertEquals(LostCardReportContext.VISITOR_IN_PARKING, result.context());
        assertEquals(session, result.parkingSession());
        assertEquals(CARD_ID, result.cardId());
        assertEquals(new BigDecimal("50000"), result.lostCardFee());
        assertEquals(result.ticketPrice().add(result.lostCardFee()), result.totalAmount());
        verify(lostCardReportAccessGuard).ensureCanRead();
    }

    @Test
    void shouldCreateVisitorLostCardReportAndInvoice() {
        ParkingSession session = visitorOpenSession();
        Card card = card(CardStatus.IN_USE);
        LostCardReport request = createVisitorReportRequest();
        stubVisitorPriceRules();

        when(parkingSessionPortOut.findById(PARKING_SESSION_ID)).thenReturn(Optional.of(session));
        when(cardPortOut.findById(CARD_ID)).thenReturn(Optional.of(card));
        when(lostCardReportPortOut.existsOpenByCardId(CARD_ID)).thenReturn(false);
        when(parkingSessionPortOut.save(any(ParkingSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cardPortOut.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lostCardReportPortOut.save(any(LostCardReport.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoicePortOut.existsByLostCardReportIdAndStatusIn(any(UUID.class), anyList())).thenReturn(false);
        when(invoicePortOut.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LostCardReportWorkflowResult result = lostCardReportUseCase.createReport(request);

        assertNotNull(result.lostCardReport().getLostCardReportId());
        assertEquals(LostCardReportContext.VISITOR_IN_PARKING, result.lostCardReport().getContext());
        assertEquals(LostCardReportStatus.OPEN, result.lostCardReport().getStatus());
        assertEquals(ParkingSessionStatus.LOST_CARD, result.parkingSession().getStatus());
        assertEquals(CardStatus.LOST, card.getStatus());
        assertEquals(InvoiceStatus.UNPAID, result.invoice().getStatus());
        assertEquals(result.lostCardReport().getLostCardReportId(), result.invoice().getLostCardReportId());
        verify(lostCardReportAccessGuard).ensureCanCreate();
    }

    @Test
    void shouldResolveVisitorLostCardReportAfterPaidInvoice() {
        LostCardReport report = openVisitorReport();
        ParkingSession session = visitorLostCardSession();
        Invoice invoice = paidLostCardInvoice();

        when(lostCardReportPortOut.findById(REPORT_ID)).thenReturn(Optional.of(report));
        when(invoicePortOut.findAll(
                isNull(),
                isNull(),
                isNull(),
                eq(REPORT_ID),
                isNull(),
                isNull(),
                isNull(),
                isNull()
        )).thenReturn(List.of(invoice));
        when(parkingSessionPortOut.findById(PARKING_SESSION_ID)).thenReturn(Optional.of(session));
        when(parkingSessionPortOut.save(any(ParkingSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(ACCOUNT_ID);
        when(lostCardReportPortOut.save(any(LostCardReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LostCardReportWorkflowResult result = lostCardReportUseCase.resolveReport(REPORT_ID, null);

        assertEquals(LostCardReportStatus.RESOLVED, result.lostCardReport().getStatus());
        assertEquals(ACCOUNT_ID, result.lostCardReport().getResolvedBy());
        assertEquals(ParkingSessionStatus.CLOSED, result.parkingSession().getStatus());
        assertEquals("OPEN", result.barrierAction());
        verify(lostCardReportAccessGuard).ensureCanUpdate();
    }

    @Test
    void shouldCancelVisitorLostCardReportAndRestoreSessionAndCard() {
        LostCardReport report = openVisitorReport();
        ParkingSession session = visitorLostCardSession();
        Card oldCard = card(CardStatus.LOST);
        Invoice invoice = unpaidLostCardInvoice();

        when(lostCardReportPortOut.findById(REPORT_ID)).thenReturn(Optional.of(report));
        when(invoicePortOut.findAll(
                nullable(UUID.class),
                nullable(UUID.class),
                nullable(UUID.class),
                eq(REPORT_ID),
                nullable(InvoiceStatus.class),
                nullable(Instant.class),
                nullable(Instant.class),
                nullable(String.class)
        )).thenReturn(List.of(invoice));
        when(paymentPortOut.existsByInvoiceIdAndStatus(INVOICE_ID, PaymentStatus.SUCCESS)).thenReturn(false);
        when(invoicePortOut.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(parkingSessionPortOut.findById(PARKING_SESSION_ID)).thenReturn(Optional.of(session));
        when(cardPortOut.findById(CARD_ID)).thenReturn(Optional.of(oldCard));
        when(cardPortOut.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(parkingSessionPortOut.save(any(ParkingSession.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(ACCOUNT_ID);
        when(lostCardReportPortOut.save(any(LostCardReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LostCardReportWorkflowResult result = lostCardReportUseCase.cancelReport(REPORT_ID, "Khach tim lai duoc the");

        assertEquals(LostCardReportStatus.CANCELLED, result.lostCardReport().getStatus());
        assertEquals(InvoiceStatus.CANCELLED, result.invoice().getStatus());
        assertEquals(ParkingSessionStatus.OPEN, result.parkingSession().getStatus());
        assertEquals(CardStatus.IN_USE, oldCard.getStatus());
        verify(lostCardReportAccessGuard).ensureCanUpdate();
    }

    @Test
    void shouldResolveRegisteredOutsideReportWithNewCard() {
        LostCardReport report = openRegisteredOutsideReport();
        Subscription subscription = activeSubscription();
        Card newCard = card(CardStatus.AVAILABLE);
        newCard.setCardId(NEW_CARD_ID);
        newCard.setVehicleTypeId(null);
        Invoice invoice = paidLostCardInvoice();

        when(lostCardReportPortOut.findById(REPORT_ID)).thenReturn(Optional.of(report));
        when(invoicePortOut.findAll(
                nullable(UUID.class),
                nullable(UUID.class),
                nullable(UUID.class),
                eq(REPORT_ID),
                nullable(InvoiceStatus.class),
                nullable(Instant.class),
                nullable(Instant.class),
                nullable(String.class)
        )).thenReturn(List.of(invoice));
        when(subscriptionPortOut.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(customerVehiclePortOut.findById(CUSTOMER_VEHICLE_ID)).thenReturn(Optional.of(customerVehicle()));
        when(cardPortOut.findById(NEW_CARD_ID)).thenReturn(Optional.of(newCard));
        when(cardPortOut.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(subscriptionPortOut.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(ACCOUNT_ID);
        when(lostCardReportPortOut.save(any(LostCardReport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LostCardReportWorkflowResult result = lostCardReportUseCase.resolveReport(REPORT_ID, NEW_CARD_ID);

        assertEquals(LostCardReportStatus.RESOLVED, result.lostCardReport().getStatus());
        assertEquals(CardStatus.ASSIGNED, newCard.getStatus());
        assertEquals(NEW_CARD_ID, result.subscription().getCardId());
        assertEquals("NONE", result.barrierAction());
    }

    private void stubVisitorPriceRules() {
        PriceRule dayRule = priceRule(new BigDecimal("4000"), new BigDecimal("50000"), LocalTime.of(6, 0), LocalTime.of(19, 59, 59));
        PriceRule nightRule = priceRule(new BigDecimal("8000"), new BigDecimal("50000"), LocalTime.of(20, 0), LocalTime.of(5, 59, 59));

        when(priceRulePortOut.findActiveVisitorRuleByTime(
                eq(VEHICLE_TYPE_ID),
                any(LocalDate.class),
                any(LocalTime.class)
        )).thenAnswer(invocation -> {
            LocalTime localTime = invocation.getArgument(2);
            return LocalTime.MIDNIGHT.equals(localTime) ? Optional.of(nightRule) : Optional.of(dayRule);
        });
    }

    private ParkingSession visitorOpenSession() {
        ParkingSession session = baseSession();
        session.setStatus(ParkingSessionStatus.OPEN);
        return session;
    }

    private ParkingSession visitorLostCardSession() {
        ParkingSession session = baseSession();
        session.setStatus(ParkingSessionStatus.LOST_CARD);
        session.setCheckOutTime(Instant.parse("2026-06-29T05:00:00Z"));
        session.setTotalPrice(new BigDecimal("4000"));
        return session;
    }

    private ParkingSession baseSession() {
        ParkingSession session = new ParkingSession();
        session.setParkingSessionId(PARKING_SESSION_ID);
        session.setCardId(CARD_ID);
        session.setVehicleTypeId(VEHICLE_TYPE_ID);
        session.setLicensePlateIn("60K8-2301");
        session.setCheckInTime(Instant.now().minusSeconds(1800));
        return session;
    }

    private LostCardReport createVisitorReportRequest() {
        LostCardReport report = new LostCardReport();
        report.setParkingSessionId(PARKING_SESSION_ID);
        report.setTimeOfLost(Instant.now().minusSeconds(600));
        report.setReporterName("Nguyen Van A");
        report.setReporterPhone("0901234567");
        report.setIdentifyCard("080112345678");
        report.setNote("Khach bao mat the");
        return report;
    }

    private LostCardReport openVisitorReport() {
        LostCardReport report = baseReport();
        report.setParkingSessionId(PARKING_SESSION_ID);
        report.setContext(LostCardReportContext.VISITOR_IN_PARKING);
        return report;
    }

    private LostCardReport openRegisteredOutsideReport() {
        LostCardReport report = baseReport();
        report.setSubscriptionId(SUBSCRIPTION_ID);
        report.setCustomerId(CUSTOMER_ID);
        report.setContext(LostCardReportContext.REGISTERED_OUTSIDE);
        report.setTicketPrice(BigDecimal.ZERO);
        return report;
    }

    private LostCardReport baseReport() {
        LostCardReport report = new LostCardReport();
        report.setLostCardReportId(REPORT_ID);
        report.setCardId(CARD_ID);
        report.setNotificationTime(Instant.now().minusSeconds(300));
        report.setTimeOfLost(Instant.now().minusSeconds(600));
        report.setTicketPrice(new BigDecimal("4000"));
        report.setLostCardFee(new BigDecimal("50000"));
        report.setReporterName("Nguyen Van A");
        report.setReporterPhone("0901234567");
        report.setIdentifyCard("080112345678");
        report.setStatus(LostCardReportStatus.OPEN);
        return report;
    }

    private Card card(CardStatus status) {
        Card card = new Card();
        card.setCardId(CARD_ID);
        card.setCardNumber("V001");
        card.setUid("RFID-001");
        card.setCardTypeId(UUID.randomUUID());
        card.setVehicleTypeId(VEHICLE_TYPE_ID);
        card.setStatus(status);
        return card;
    }

    private PriceRule priceRule(BigDecimal basePrice, BigDecimal lostCardFee, LocalTime timeFrom, LocalTime timeTo) {
        PriceRule priceRule = new PriceRule();
        priceRule.setPriceRuleId(PRICE_RULE_ID);
        priceRule.setVehicleTypeId(VEHICLE_TYPE_ID);
        priceRule.setBasePrice(basePrice);
        priceRule.setLostCardFee(lostCardFee);
        priceRule.setTimeFrom(timeFrom);
        priceRule.setTimeTo(timeTo);
        priceRule.setUnit(PriceRuleUnit.TURN);
        priceRule.setIsActive(true);
        return priceRule;
    }

    private Invoice unpaidLostCardInvoice() {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(INVOICE_ID);
        invoice.setInvoiceNo("INV-20260629120000-TEST0001");
        invoice.setLostCardReportId(REPORT_ID);
        invoice.setAmount(new BigDecimal("54000"));
        invoice.setDiscountAmount(BigDecimal.ZERO);
        invoice.setFinalAmount(new BigDecimal("54000"));
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setIssuedAt(Instant.now().minusSeconds(120));
        return invoice;
    }

    private Invoice paidLostCardInvoice() {
        Invoice invoice = unpaidLostCardInvoice();
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(Instant.now().minusSeconds(60));
        return invoice;
    }

    private Subscription activeSubscription() {
        Subscription subscription = new Subscription();
        subscription.setSubscriptionId(SUBSCRIPTION_ID);
        subscription.setCustomerId(CUSTOMER_ID);
        subscription.setCustomerVehicleId(CUSTOMER_VEHICLE_ID);
        subscription.setCardId(CARD_ID);
        subscription.setPriceRuleId(PRICE_RULE_ID);
        subscription.setEffectiveFrom(LocalDate.now().minusDays(1));
        subscription.setEffectiveTo(LocalDate.now().plusDays(30));
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        return subscription;
    }

    private CustomerVehicle customerVehicle() {
        CustomerVehicle customerVehicle = new CustomerVehicle();
        customerVehicle.setCustomerVehicleId(CUSTOMER_VEHICLE_ID);
        customerVehicle.setCustomerId(CUSTOMER_ID);
        customerVehicle.setVehicleTypeId(VEHICLE_TYPE_ID);
        customerVehicle.setLicensePlate("60K8-2301");
        return customerVehicle;
    }
}
