package com.ban.vehicle_management.application.accesscontrol.subscription.usecase;

import com.ban.vehicle_management.application.accesscontrol.card.port.out.CardPortOut;
import com.ban.vehicle_management.application.accesscontrol.subscription.authorization.SubscriptionAccessGuard;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.in.SubscriptionPortIn;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionPortOut;
import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.application.catalog.pricerule.port.out.PriceRulePortOut;
import com.ban.vehicle_management.application.catalog.tickettype.port.out.TicketTypePortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.parking.zone.port.out.ZonePortOut;
import com.ban.vehicle_management.application.people.customer.port.out.CustomerPortOut;
import com.ban.vehicle_management.application.people.customervehicle.port.out.CustomerVehiclePortOut;
import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.domain.accesscontrol.card.policy.CardPolicy;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.accesscontrol.subscription.policy.SubscriptionPolicy;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.billing.invoice.policy.InvoicePolicy;
import com.ban.vehicle_management.domain.catalog.pricerule.model.PriceRule;
import com.ban.vehicle_management.domain.catalog.tickettype.model.TicketType;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import com.ban.vehicle_management.shared.enumeration.catalog.TicketTypeStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerApprovalStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerStatus;
import com.ban.vehicle_management.shared.enumeration.people.CustomerVehicleStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SubscriptionUseCaseImpl implements SubscriptionPortIn {

    private static final Set<String> ALLOWED_SUBSCRIPTION_TICKET_CODES =
            Set.of("MONTHLY", "QUARTERLY", "YEARLY", "FREE");

    private static final DateTimeFormatter INVOICE_NO_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyyMMddHHmmss")
            .withZone(DateTimeUtils.VIETNAM_ZONE);

    private static final List<InvoiceStatus> ACTIVE_INVOICE_STATUSES = List.of(
            InvoiceStatus.UNPAID,
            InvoiceStatus.PAID
    );

    private final SubscriptionPortOut subscriptionPortOut;
    private final CustomerVehiclePortOut customerVehiclePortOut;
    private final CustomerPortOut customerPortOut;
    private final TicketTypePortOut ticketTypePortOut;
    private final PriceRulePortOut priceRulePortOut;
    private final CardPortOut cardPortOut;
    private final InvoicePortOut invoicePortOut;
    private final ZonePortOut zonePortOut;
    private final CurrentAccountPortIn currentAccountPortIn;
    private final SubscriptionAccessGuard subscriptionAccessGuard;
    private final SubscriptionPolicy subscriptionPolicy = new SubscriptionPolicy();
    private final CardPolicy cardPolicy = new CardPolicy();
    private final InvoicePolicy invoicePolicy = new InvoicePolicy();

    public SubscriptionUseCaseImpl(
            SubscriptionPortOut subscriptionPortOut,
            CustomerVehiclePortOut customerVehiclePortOut,
            CustomerPortOut customerPortOut,
            TicketTypePortOut ticketTypePortOut,
            PriceRulePortOut priceRulePortOut,
            CardPortOut cardPortOut,
            InvoicePortOut invoicePortOut,
            ZonePortOut zonePortOut,
            CurrentAccountPortIn currentAccountPortIn,
            SubscriptionAccessGuard subscriptionAccessGuard
    ) {
        this.subscriptionPortOut = subscriptionPortOut;
        this.customerVehiclePortOut = customerVehiclePortOut;
        this.customerPortOut = customerPortOut;
        this.ticketTypePortOut = ticketTypePortOut;
        this.priceRulePortOut = priceRulePortOut;
        this.cardPortOut = cardPortOut;
        this.invoicePortOut = invoicePortOut;
        this.zonePortOut = zonePortOut;
        this.currentAccountPortIn = currentAccountPortIn;
        this.subscriptionAccessGuard = subscriptionAccessGuard;
    }

    @Override
    @Transactional
    public Subscription createOwnSubscription(Subscription subscription) {
        subscriptionAccessGuard.ensureCanCreateOwn();
        UUID customerId = subscriptionAccessGuard.resolveCurrentApprovedCustomerId();
        subscription.setCustomerId(customerId);
        return createPendingSubscription(subscription);
    }

    @Override
    @Transactional
    public Subscription createSubscriptionForCustomer(Subscription subscription) {
        subscriptionAccessGuard.ensureCanCreateAll();
        requireField(subscription.getCustomerId(), "customerId");
        return createPendingSubscription(subscription);
    }

    @Override
    @Transactional(readOnly = true)
    public Subscription getSubscriptionById(UUID subscriptionId) {
        Subscription subscription = findSubscriptionOrThrow(subscriptionId);
        subscriptionAccessGuard.ensureCanRead(subscription);
        return subscription;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Subscription> getSubscriptions(
            UUID customerId,
            UUID customerVehicleId,
            UUID cardId,
            UUID ticketTypeId,
            com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus status,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String keyword
    ) {
        UUID readableCustomerId = subscriptionAccessGuard.resolveCustomerIdForList(customerId);
        return subscriptionPortOut.findAll(
                readableCustomerId,
                customerVehicleId,
                cardId,
                ticketTypeId,
                status,
                effectiveFrom,
                effectiveTo,
                normalizeKeyword(keyword)
        );
    }

    @Override
    @Transactional
    public Subscription updatePendingSubscription(UUID subscriptionId, Subscription requestedSubscription) {
        Subscription existingSubscription = findSubscriptionOrThrow(subscriptionId);
        subscriptionAccessGuard.ensureCanUpdate(existingSubscription);

        existingSubscription.setCustomerVehicleId(requestedSubscription.getCustomerVehicleId());
        existingSubscription.setTicketTypeId(requestedSubscription.getTicketTypeId());
        existingSubscription.setRequestedEffectiveFrom(requestedSubscription.getRequestedEffectiveFrom());

        SubscriptionPreparedData preparedData = prepareSubscriptionData(existingSubscription);
        existingSubscription.setPriceRuleId(preparedData.priceRule().getPriceRuleId());
        existingSubscription.setPrice(preparedData.priceRule().getBasePrice());

        subscriptionPolicy.preparePendingUpdate(
                existingSubscription,
                preparedData.ticketType().getDurationDays(),
                currentDate()
        );

        ensureNoOverlappingSubscription(existingSubscription, subscriptionId);
        return subscriptionPortOut.save(existingSubscription);
    }

    @Override
    @Transactional
    public Subscription approveSubscription(UUID subscriptionId) {
        subscriptionAccessGuard.ensureCanApprove();

        Subscription subscription = findSubscriptionOrThrow(subscriptionId);
        Instant now = Instant.now();
        LocalDate today = currentDate();

        if (!today.isBefore(subscription.getRequestedEffectiveFrom())) {
            subscriptionPolicy.reject(
                    subscription,
                    "Approval deadline expired",
                    currentAccountPortIn.getCurrentAccountIdOrThrow(),
                    now
            );
            return subscriptionPortOut.save(subscription);
        }

        SubscriptionPreparedData preparedData = prepareSubscriptionData(subscription);
        ensureNoOverlappingSubscription(subscription, subscriptionId);
        ensureCapacityAvailable(preparedData.customerVehicle().getVehicleTypeId());

        if (invoicePortOut.existsBySubscriptionIdAndStatusIn(subscriptionId, ACTIVE_INVOICE_STATUSES)) {
            throw new ConflictException("Active invoice already exists for subscription");
        }

        Card reservedCard = cardPortOut.findFirstAvailableRegisteredByVehicleTypeId(preparedData.customerVehicle().getVehicleTypeId())
                .orElseThrow(() -> new ConflictException("No available registered card for vehicle type"));

        cardPolicy.reserve(reservedCard);
        Card savedReservedCard = cardPortOut.save(reservedCard);

        BigDecimal invoiceFinalAmount = subscription.getPrice();
        subscriptionPolicy.approve(
                subscription,
                currentAccountPortIn.getCurrentAccountIdOrThrow(),
                now,
                today,
                savedReservedCard.getCardId(),
                invoiceFinalAmount
        );

        Subscription approvedSubscription = subscriptionPortOut.save(subscription);
        invoicePortOut.save(buildSubscriptionInvoice(approvedSubscription, now));

        return approvedSubscription;
    }

    @Override
    @Transactional
    public Subscription rejectSubscription(UUID subscriptionId, String reason) {
        subscriptionAccessGuard.ensureCanReject();

        Subscription subscription = findSubscriptionOrThrow(subscriptionId);
        subscriptionPolicy.reject(
                subscription,
                reason,
                currentAccountPortIn.getCurrentAccountIdOrThrow(),
                Instant.now()
        );

        return subscriptionPortOut.save(subscription);
    }

    @Override
    @Transactional
    public Subscription markSubscriptionPaymentCompleted(UUID subscriptionId) {
        Subscription subscription = findSubscriptionOrThrow(subscriptionId);

        invoicePortOut.findFirstBySubscriptionIdAndStatus(subscriptionId, InvoiceStatus.PAID)
                .orElseThrow(() -> new ConflictException("Paid invoice not found for subscription"));

        subscriptionPolicy.markPaymentCompleted(subscription);
        return subscriptionPortOut.save(subscription);
    }

    @Override
    @Transactional
    public Subscription assignReservedCard(UUID subscriptionId) {
        subscriptionAccessGuard.ensureCanAssignCard();

        Subscription subscription = findSubscriptionOrThrow(subscriptionId);
        TicketType ticketType = findActiveSubscriptionTicketType(subscription.getTicketTypeId());

        invoicePortOut.findFirstBySubscriptionIdAndStatus(subscriptionId, InvoiceStatus.PAID)
                .orElseThrow(() -> new ConflictException("Paid invoice not found for subscription"));

        Card reservedCard = cardPortOut.findById(subscription.getCardId())
                .orElseThrow(() -> new NotFoundException("Reserved card not found"));

        Instant now = Instant.now();
        cardPolicy.assignReserved(reservedCard, now);
        subscriptionPolicy.assignReservedCard(subscription, currentDate(), ticketType.getDurationDays());

        cardPortOut.save(reservedCard);
        return subscriptionPortOut.save(subscription);
    }

    @Override
    @Transactional
    public Subscription cancelSubscription(UUID subscriptionId) {
        Subscription subscription = findSubscriptionOrThrow(subscriptionId);
        subscriptionAccessGuard.ensureCanCancel(subscription);

        if (subscription.getStatus() == com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus.PENDING_PAYMENT) {
            Invoice unpaidInvoice = invoicePortOut.findFirstBySubscriptionIdAndStatus(subscriptionId, InvoiceStatus.UNPAID)
                    .orElseThrow(() -> new ConflictException("Unpaid invoice not found for subscription"));

            invoicePolicy.cancel(unpaidInvoice);
            invoicePortOut.save(unpaidInvoice);

            if (subscription.getCardId() != null) {
                Card reservedCard = cardPortOut.findById(subscription.getCardId())
                        .orElseThrow(() -> new NotFoundException("Reserved card not found"));
                cardPolicy.release(reservedCard);
                cardPortOut.save(reservedCard);
                subscription.setCardId(null);
            }
        }

        subscriptionPolicy.cancelBeforeRefundWorkflow(subscription);
        return subscriptionPortOut.save(subscription);
    }

    @Override
    @Transactional
    public Subscription expireSubscription(UUID subscriptionId) {
        subscriptionAccessGuard.ensureCanExpire();

        Subscription subscription = findSubscriptionOrThrow(subscriptionId);
        subscriptionPolicy.expire(subscription, currentDate());

        return subscriptionPortOut.save(subscription);
    }

    private Subscription createPendingSubscription(Subscription subscription) {
        subscription.setSubscriptionId(UUID.randomUUID());

        SubscriptionPreparedData preparedData = prepareSubscriptionData(subscription);
        subscription.setPriceRuleId(preparedData.priceRule().getPriceRuleId());
        subscription.setPrice(preparedData.priceRule().getBasePrice());

        subscriptionPolicy.initializeNewSubscription(
                subscription,
                preparedData.ticketType().getDurationDays(),
                currentDate()
        );

        ensureNoOverlappingSubscription(subscription, null);
        return subscriptionPortOut.save(subscription);
    }

    private SubscriptionPreparedData prepareSubscriptionData(Subscription subscription) {
        Customer customer = findActiveApprovedCustomer(subscription.getCustomerId());

        CustomerVehicle customerVehicle = customerVehiclePortOut.findById(subscription.getCustomerVehicleId())
                .orElseThrow(() -> new NotFoundException("Customer vehicle not found"));

        if (!customer.getCustomerId().equals(customerVehicle.getCustomerId())) {
            throw new BadRequestException("Customer vehicle does not belong to customer");
        }

        if (customerVehicle.getStatus() != CustomerVehicleStatus.ACTIVE) {
            throw new ConflictException("Customer vehicle is not active");
        }

        TicketType ticketType = findActiveSubscriptionTicketType(subscription.getTicketTypeId());

        PriceRule priceRule = priceRulePortOut.findActiveSubscriptionRule(
                        customerVehicle.getVehicleTypeId(),
                        ticketType.getTicketTypeId(),
                        subscription.getRequestedEffectiveFrom()
                )
                .orElseThrow(() -> new NotFoundException("Active subscription price rule not found"));

        if (priceRule.getBasePrice() == null || priceRule.getBasePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Subscription price rule price is invalid");
        }

        return new SubscriptionPreparedData(customer, customerVehicle, ticketType, priceRule);
    }

    private Customer findActiveApprovedCustomer(UUID customerId) {
        requireField(customerId, "customerId");

        Customer customer = customerPortOut.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));

        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            throw new ConflictException("Customer is not active");
        }

        if (customer.getApprovalStatus() != CustomerApprovalStatus.APPROVED) {
            throw new ConflictException("Customer is not approved");
        }

        return customer;
    }

    private TicketType findActiveSubscriptionTicketType(UUID ticketTypeId) {
        requireField(ticketTypeId, "ticketTypeId");

        TicketType ticketType = ticketTypePortOut.findById(ticketTypeId)
                .orElseThrow(() -> new NotFoundException("Ticket type not found"));

        if (ticketType.getStatus() != TicketTypeStatus.ACTIVE) {
            throw new ConflictException("Ticket type is not active");
        }

        if (ticketType.getDurationDays() == null || ticketType.getDurationDays() <= 0) {
            throw new BadRequestException("Ticket type durationDays must be greater than zero");
        }

        if (ticketType.getCode() == null || !ALLOWED_SUBSCRIPTION_TICKET_CODES.contains(ticketType.getCode().trim().toUpperCase())) {
            throw new BadRequestException("Subscription only accepts MONTHLY, QUARTERLY, YEARLY or FREE ticket type");
        }

        return ticketType;
    }

    private void ensureNoOverlappingSubscription(Subscription subscription, UUID excludedSubscriptionId) {
        if (subscriptionPortOut.existsOverlappingSubscription(
                subscription.getCustomerVehicleId(),
                subscription.getEffectiveFrom(),
                subscription.getEffectiveTo(),
                excludedSubscriptionId
        )) {
            throw new ConflictException("Customer vehicle already has an active or pending subscription in this period");
        }
    }

    private void ensureCapacityAvailable(UUID vehicleTypeId) {
        long activeCapacity = zonePortOut.sumActiveCapacityByVehicleTypeId(vehicleTypeId);
        if (activeCapacity <= 0) {
            throw new ConflictException("No active zone capacity available for vehicle type");
        }

        long usedCapacity = subscriptionPortOut.countReservedOrActiveByVehicleTypeId(vehicleTypeId);
        if (usedCapacity >= activeCapacity) {
            throw new ConflictException("No available subscription capacity for vehicle type");
        }
    }

    private Invoice buildSubscriptionInvoice(Subscription subscription, Instant now) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(UUID.randomUUID());
        invoice.setCustomerId(subscription.getCustomerId());
        invoice.setSubscriptionId(subscription.getSubscriptionId());
        invoice.setAmount(subscription.getPrice());
        invoice.setDiscountAmount(BigDecimal.ZERO);

        invoicePolicy.initializeNewInvoice(
                invoice,
                generateInvoiceNo(invoice.getInvoiceId(), now),
                now
        );

        return invoice;
    }

    private String generateInvoiceNo(UUID invoiceId, Instant now) {
        String suffix = invoiceId.toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();

        return "INV-" + INVOICE_NO_TIME_FORMATTER.format(now) + "-" + suffix;
    }

    private Subscription findSubscriptionOrThrow(UUID subscriptionId) {
        return subscriptionPortOut.findById(subscriptionId)
                .orElseThrow(() -> new NotFoundException("Subscription not found"));
    }

    private LocalDate currentDate() {
        return LocalDate.now(DateTimeUtils.VIETNAM_ZONE);
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }

    private record SubscriptionPreparedData(
            Customer customer,
            CustomerVehicle customerVehicle,
            TicketType ticketType,
            PriceRule priceRule
    ) {
    }
}