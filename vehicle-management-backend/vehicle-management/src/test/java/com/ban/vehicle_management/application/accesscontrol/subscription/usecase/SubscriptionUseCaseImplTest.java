package com.ban.vehicle_management.application.accesscontrol.subscription.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.accesscontrol.card.port.out.CardPortOut;
import com.ban.vehicle_management.application.accesscontrol.subscription.authorization.SubscriptionAccessGuard;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionPortOut;
import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.application.catalog.pricerule.port.out.PriceRulePortOut;
import com.ban.vehicle_management.application.catalog.tickettype.port.out.TicketTypePortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.application.people.customer.port.out.CustomerPortOut;
import com.ban.vehicle_management.application.people.customervehicle.port.out.CustomerVehiclePortOut;
import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.catalog.pricerule.model.PriceRule;
import com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import com.ban.vehicle_management.shared.enumeration.catalog.TicketTypeStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerVehicleStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionUseCaseImplTest {

    @Mock
    private SubscriptionPortOut subscriptionPortOut;

    @Mock
    private CustomerVehiclePortOut customerVehiclePortOut;

    @Mock
    private CustomerPortOut customerPortOut;

    @Mock
    private TicketTypePortOut ticketTypePortOut;

    @Mock
    private PriceRulePortOut priceRulePortOut;

    @Mock
    private CardPortOut cardPortOut;

    @Mock
    private InvoicePortOut invoicePortOut;

    @Mock
    private ZonePortOut zonePortOut;

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @Mock
    private SubscriptionAccessGuard subscriptionAccessGuard;

    @InjectMocks
    private SubscriptionUseCaseImpl subscriptionUseCase;

    @Test
    void shouldCreateOwnSubscriptionWhenDataIsValid() {
        TestData data = validTestData();
        Subscription request = createRequest(data.customerVehicleId(), data.ticketTypeId());

        when(subscriptionAccessGuard.resolveCurrentApprovedCustomerId()).thenReturn(data.customerId());
        mockValidSubscriptionPreparation(data);
        when(subscriptionPortOut.existsOverlappingSubscription(data.customerVehicleId(), request.getRequestedEffectiveFrom(), request.getRequestedEffectiveFrom().plusDays(29), null))
                .thenReturn(false);
        when(subscriptionPortOut.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Subscription createdSubscription = subscriptionUseCase.createOwnSubscription(request);

        verify(subscriptionAccessGuard).ensureCanCreateOwn();
        assertNotNull(createdSubscription.getSubscriptionId());
        assertEquals(data.customerId(), createdSubscription.getCustomerId());
        assertEquals(data.priceRuleId(), createdSubscription.getPriceRuleId());
        assertEquals(new BigDecimal("140000"), createdSubscription.getPrice());
        assertEquals(SubscriptionStatus.PENDING, createdSubscription.getStatus());
        assertEquals(request.getRequestedEffectiveFrom(), createdSubscription.getEffectiveFrom());
        assertEquals(request.getRequestedEffectiveFrom().plusDays(29), createdSubscription.getEffectiveTo());
    }

    @Test
    void shouldRejectCreateWhenVehicleDoesNotBelongToCustomer() {
        TestData data = validTestData();
        Subscription request = createRequest(data.customerVehicleId(), data.ticketTypeId());
        CustomerVehicle vehicle = activeCustomerVehicle(data.customerVehicleId(), UUID.randomUUID(), data.vehicleTypeId());

        when(subscriptionAccessGuard.resolveCurrentApprovedCustomerId()).thenReturn(data.customerId());
        when(customerPortOut.findById(data.customerId())).thenReturn(Optional.of(activeApprovedCustomer(data.customerId())));
        when(customerVehiclePortOut.findById(data.customerVehicleId())).thenReturn(Optional.of(vehicle));

        assertThrows(BadRequestException.class, () -> subscriptionUseCase.createOwnSubscription(request));
        verify(subscriptionPortOut, never()).save(any(Subscription.class));
    }

    @Test
    void shouldReturnFilteredSubscriptionsWithResolvedReadableCustomerId() {
        UUID requestedCustomerId = UUID.randomUUID();
        UUID readableCustomerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        UUID ticketTypeId = UUID.randomUUID();
        List<Subscription> expectedSubscriptions = List.of(new Subscription(), new Subscription());

        when(subscriptionAccessGuard.resolveCustomerIdForList(requestedCustomerId)).thenReturn(readableCustomerId);
        when(subscriptionPortOut.findAll(
                readableCustomerId,
                vehicleId,
                cardId,
                ticketTypeId,
                SubscriptionStatus.PENDING,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                "SUB"
        )).thenReturn(expectedSubscriptions);

        List<Subscription> subscriptions = subscriptionUseCase.getSubscriptions(
                requestedCustomerId,
                vehicleId,
                cardId,
                ticketTypeId,
                SubscriptionStatus.PENDING,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                " SUB "
        );

        assertEquals(expectedSubscriptions, subscriptions);
    }

    @Test
    void shouldApproveSubscriptionAndCreateInvoiceAndReserveCard() {
        TestData data = validTestData();
        UUID managerAccountId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        Subscription pendingSubscription = pendingSubscription(data);
        Card availableCard = availableCard(cardId, data.vehicleTypeId());

        when(subscriptionPortOut.findById(data.subscriptionId())).thenReturn(Optional.of(pendingSubscription));
        mockValidSubscriptionPreparation(data);
        when(subscriptionPortOut.existsOverlappingSubscription(data.customerVehicleId(), pendingSubscription.getEffectiveFrom(), pendingSubscription.getEffectiveTo(), data.subscriptionId()))
                .thenReturn(false);
        when(zonePortOut.sumActiveCapacityByVehicleTypeId(data.vehicleTypeId())).thenReturn(100L);
        when(subscriptionPortOut.countReservedOrActiveByVehicleTypeId(data.vehicleTypeId())).thenReturn(10L);
        when(invoicePortOut.existsBySubscriptionIdAndStatusIn(eq(data.subscriptionId()), any(List.class))).thenReturn(false);
        when(cardPortOut.findFirstAvailableRegisteredByVehicleTypeId(data.vehicleTypeId())).thenReturn(Optional.of(availableCard));
        when(cardPortOut.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(managerAccountId);
        when(subscriptionPortOut.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoicePortOut.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Subscription approvedSubscription = subscriptionUseCase.approveSubscription(data.subscriptionId());

        verify(subscriptionAccessGuard).ensureCanApprove();
        assertEquals(SubscriptionStatus.PENDING_PAYMENT, approvedSubscription.getStatus());
        assertEquals(cardId, approvedSubscription.getCardId());
        assertEquals(managerAccountId, approvedSubscription.getApprovedBy());
        assertEquals(CardStatus.RESERVED, availableCard.getStatus());

        ArgumentCaptor<Invoice> invoiceCaptor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoicePortOut).save(invoiceCaptor.capture());
        Invoice savedInvoice = invoiceCaptor.getValue();
        assertEquals(data.subscriptionId(), savedInvoice.getSubscriptionId());
        assertEquals(data.customerId(), savedInvoice.getCustomerId());
        assertEquals(new BigDecimal("140000"), savedInvoice.getAmount());
        assertEquals(InvoiceStatus.UNPAID, savedInvoice.getStatus());
    }

    @Test
    void shouldRejectApproveWhenNoAvailableCardExists() {
        TestData data = validTestData();
        Subscription pendingSubscription = pendingSubscription(data);

        when(subscriptionPortOut.findById(data.subscriptionId())).thenReturn(Optional.of(pendingSubscription));
        mockValidSubscriptionPreparation(data);
        when(subscriptionPortOut.existsOverlappingSubscription(data.customerVehicleId(), pendingSubscription.getEffectiveFrom(), pendingSubscription.getEffectiveTo(), data.subscriptionId()))
                .thenReturn(false);
        when(zonePortOut.sumActiveCapacityByVehicleTypeId(data.vehicleTypeId())).thenReturn(100L);
        when(subscriptionPortOut.countReservedOrActiveByVehicleTypeId(data.vehicleTypeId())).thenReturn(10L);
        when(invoicePortOut.existsBySubscriptionIdAndStatusIn(eq(data.subscriptionId()), any(List.class))).thenReturn(false);

        assertThrows(ConflictException.class, () -> subscriptionUseCase.approveSubscription(data.subscriptionId()));
        verify(subscriptionPortOut, never()).save(any(Subscription.class));
        verify(invoicePortOut, never()).save(any(Invoice.class));
    }

    @Test
    void shouldMarkSubscriptionPaymentCompletedWhenPaidInvoiceExists() {
        TestData data = validTestData();
        Subscription pendingPaymentSubscription = approvedSubscription(data, SubscriptionStatus.PENDING_PAYMENT);

        when(subscriptionPortOut.findById(data.subscriptionId())).thenReturn(Optional.of(pendingPaymentSubscription));
        when(invoicePortOut.findFirstBySubscriptionIdAndStatus(data.subscriptionId(), InvoiceStatus.PAID))
                .thenReturn(Optional.of(paidInvoice(data)));
        when(subscriptionPortOut.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Subscription updatedSubscription = subscriptionUseCase.markSubscriptionPaymentCompleted(data.subscriptionId());

        assertEquals(SubscriptionStatus.PENDING_CARD, updatedSubscription.getStatus());
        verify(subscriptionPortOut).save(pendingPaymentSubscription);
    }

    @Test
    void shouldRejectPaymentCompletedWhenPaidInvoiceDoesNotExist() {
        TestData data = validTestData();
        Subscription pendingPaymentSubscription = approvedSubscription(data, SubscriptionStatus.PENDING_PAYMENT);

        when(subscriptionPortOut.findById(data.subscriptionId())).thenReturn(Optional.of(pendingPaymentSubscription));
        when(invoicePortOut.findFirstBySubscriptionIdAndStatus(data.subscriptionId(), InvoiceStatus.PAID))
                .thenReturn(Optional.empty());

        assertThrows(ConflictException.class, () -> subscriptionUseCase.markSubscriptionPaymentCompleted(data.subscriptionId()));
        verify(subscriptionPortOut, never()).save(any(Subscription.class));
    }

    @Test
    void shouldAssignReservedCardWhenPaidInvoiceExists() {
        TestData data = validTestData();
        UUID cardId = UUID.randomUUID();
        Subscription pendingCardSubscription = approvedSubscription(data, SubscriptionStatus.PENDING_CARD);
        pendingCardSubscription.setCardId(cardId);
        Card reservedCard = reservedCard(cardId, data.vehicleTypeId());

        when(subscriptionPortOut.findById(data.subscriptionId())).thenReturn(Optional.of(pendingCardSubscription));
        when(ticketTypePortOut.findById(data.ticketTypeId())).thenReturn(Optional.of(activeMonthlyTicketType(data.ticketTypeId())));
        when(invoicePortOut.findFirstBySubscriptionIdAndStatus(data.subscriptionId(), InvoiceStatus.PAID))
                .thenReturn(Optional.of(paidInvoice(data)));
        when(cardPortOut.findById(cardId)).thenReturn(Optional.of(reservedCard));
        when(cardPortOut.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(subscriptionPortOut.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Subscription activeSubscription = subscriptionUseCase.assignReservedCard(data.subscriptionId());

        verify(subscriptionAccessGuard).ensureCanAssignCard();
        assertEquals(SubscriptionStatus.ACTIVE, activeSubscription.getStatus());
        assertNotNull(activeSubscription.getCardReceiptDate());
        assertEquals(CardStatus.ASSIGNED, reservedCard.getStatus());
        assertNotNull(reservedCard.getIssuedAt());
    }

    @Test
    void shouldCancelPendingPaymentSubscriptionAndReleaseReservedCard() {
        TestData data = validTestData();
        UUID cardId = UUID.randomUUID();
        Subscription pendingPaymentSubscription = approvedSubscription(data, SubscriptionStatus.PENDING_PAYMENT);
        pendingPaymentSubscription.setCardId(cardId);
        Invoice unpaidInvoice = unpaidInvoice(data);
        Card reservedCard = reservedCard(cardId, data.vehicleTypeId());

        when(subscriptionPortOut.findById(data.subscriptionId())).thenReturn(Optional.of(pendingPaymentSubscription));
        when(invoicePortOut.findFirstBySubscriptionIdAndStatus(data.subscriptionId(), InvoiceStatus.UNPAID))
                .thenReturn(Optional.of(unpaidInvoice));
        when(cardPortOut.findById(cardId)).thenReturn(Optional.of(reservedCard));
        when(subscriptionPortOut.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Subscription cancelledSubscription = subscriptionUseCase.cancelSubscription(data.subscriptionId());

        verify(subscriptionAccessGuard).ensureCanCancel(pendingPaymentSubscription);
        assertEquals(SubscriptionStatus.CANCELLED, cancelledSubscription.getStatus());
        assertNull(cancelledSubscription.getCardId());
        assertEquals(InvoiceStatus.CANCELLED, unpaidInvoice.getStatus());
        assertEquals(CardStatus.AVAILABLE, reservedCard.getStatus());
        verify(invoicePortOut).save(unpaidInvoice);
        verify(cardPortOut).save(reservedCard);
    }

    private void mockValidSubscriptionPreparation(TestData data) {
        when(customerPortOut.findById(data.customerId())).thenReturn(Optional.of(activeApprovedCustomer(data.customerId())));
        when(customerVehiclePortOut.findById(data.customerVehicleId()))
                .thenReturn(Optional.of(activeCustomerVehicle(data.customerVehicleId(), data.customerId(), data.vehicleTypeId())));
        when(ticketTypePortOut.findById(data.ticketTypeId())).thenReturn(Optional.of(activeMonthlyTicketType(data.ticketTypeId())));
        when(priceRulePortOut.findActiveSubscriptionRule(data.vehicleTypeId(), data.ticketTypeId(), requestedEffectiveFrom()))
                .thenReturn(Optional.of(subscriptionPriceRule(data.priceRuleId(), data.vehicleTypeId(), data.ticketTypeId())));
    }

    private Subscription createRequest(UUID customerVehicleId, UUID ticketTypeId) {
        Subscription subscription = new Subscription();
        subscription.setCustomerVehicleId(customerVehicleId);
        subscription.setTicketTypeId(ticketTypeId);
        subscription.setRequestedEffectiveFrom(requestedEffectiveFrom());
        return subscription;
    }

    private Subscription pendingSubscription(TestData data) {
        Subscription subscription = createRequest(data.customerVehicleId(), data.ticketTypeId());
        subscription.setSubscriptionId(data.subscriptionId());
        subscription.setCustomerId(data.customerId());
        subscription.setPriceRuleId(data.priceRuleId());
        subscription.setEffectiveFrom(requestedEffectiveFrom());
        subscription.setEffectiveTo(requestedEffectiveFrom().plusDays(29));
        subscription.setPrice(new BigDecimal("140000"));
        subscription.setStatus(SubscriptionStatus.PENDING);
        return subscription;
    }

    private Subscription approvedSubscription(TestData data, SubscriptionStatus status) {
        Subscription subscription = pendingSubscription(data);
        subscription.setStatus(status);
        subscription.setApprovedBy(UUID.randomUUID());
        subscription.setApprovedAt(Instant.now());
        subscription.setCardId(UUID.randomUUID());
        return subscription;
    }

    private Customer activeApprovedCustomer(UUID customerId) {
        Customer customer = new Customer();
        customer.setCustomerId(customerId);
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setApprovalStatus(CustomerApprovalStatus.APPROVED);
        return customer;
    }

    private CustomerVehicle activeCustomerVehicle(UUID customerVehicleId, UUID customerId, UUID vehicleTypeId) {
        CustomerVehicle customerVehicle = new CustomerVehicle();
        customerVehicle.setCustomerVehicleId(customerVehicleId);
        customerVehicle.setCustomerId(customerId);
        customerVehicle.setVehicleTypeId(vehicleTypeId);
        customerVehicle.setLicensePlate("59A1-12345");
        customerVehicle.setStatus(CustomerVehicleStatus.ACTIVE);
        return customerVehicle;
    }

    private TicketType activeMonthlyTicketType(UUID ticketTypeId) {
        TicketType ticketType = new TicketType();
        ticketType.setTicketTypeId(ticketTypeId);
        ticketType.setCode("MONTHLY");
        ticketType.setDurationDays(30);
        ticketType.setStatus(TicketTypeStatus.ACTIVE);
        return ticketType;
    }

    private PriceRule subscriptionPriceRule(UUID priceRuleId, UUID vehicleTypeId, UUID ticketTypeId) {
        PriceRule priceRule = new PriceRule();
        priceRule.setPriceRuleId(priceRuleId);
        priceRule.setVehicleTypeId(vehicleTypeId);
        priceRule.setTicketTypeId(ticketTypeId);
        priceRule.setBasePrice(new BigDecimal("140000"));
        priceRule.setPriority(1);
        priceRule.setIsActive(true);
        return priceRule;
    }

    private Card availableCard(UUID cardId, UUID vehicleTypeId) {
        Card card = new Card();
        card.setCardId(cardId);
        card.setCardNumber("V001");
        card.setUid("RFID-001");
        card.setCardTypeId(UUID.randomUUID());
        card.setVehicleTypeId(vehicleTypeId);
        card.setStatus(CardStatus.AVAILABLE);
        return card;
    }

    private Card reservedCard(UUID cardId, UUID vehicleTypeId) {
        Card card = availableCard(cardId, vehicleTypeId);
        card.setStatus(CardStatus.RESERVED);
        return card;
    }

    private Invoice unpaidInvoice(TestData data) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(UUID.randomUUID());
        invoice.setInvoiceNo("INV-001");
        invoice.setCustomerId(data.customerId());
        invoice.setSubscriptionId(data.subscriptionId());
        invoice.setAmount(new BigDecimal("140000"));
        invoice.setDiscountAmount(BigDecimal.ZERO);
        invoice.setFinalAmount(new BigDecimal("140000"));
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setIssuedAt(Instant.now());
        return invoice;
    }

    private Invoice paidInvoice(TestData data) {
        Invoice invoice = unpaidInvoice(data);
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(Instant.now());
        return invoice;
    }

    private LocalDate requestedEffectiveFrom() {
        return LocalDate.now(DateTimeUtils.VIETNAM_ZONE).plusDays(4);
    }

    private TestData validTestData() {
        return new TestData(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );
    }

    private record TestData(
            UUID customerId,
            UUID customerVehicleId,
            UUID vehicleTypeId,
            UUID ticketTypeId,
            UUID priceRuleId,
            UUID subscriptionId
    ) {
    }
}
