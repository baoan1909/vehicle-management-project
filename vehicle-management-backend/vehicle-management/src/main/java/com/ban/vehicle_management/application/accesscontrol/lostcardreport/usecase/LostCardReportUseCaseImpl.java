package com.ban.vehicle_management.application.accesscontrol.lostcardreport.usecase;

import com.ban.vehicle_management.application.accesscontrol.card.port.out.CardPortOut;
import com.ban.vehicle_management.application.accesscontrol.lostcardreport.authorization.LostCardReportAccessGuard;
import com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result.LostCardPreviewResult;
import com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result.LostCardReplacementCardResult;
import com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result.LostCardReportDetailResult;
import com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result.LostCardReportListItemResult;
import com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result.LostCardReportSummaryResult;
import com.ban.vehicle_management.application.accesscontrol.lostcardreport.model.result.LostCardReportWorkflowResult;
import com.ban.vehicle_management.application.accesscontrol.lostcardreport.port.in.LostCardReportPortIn;
import com.ban.vehicle_management.application.accesscontrol.lostcardreport.port.out.LostCardReportPortOut;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionPortOut;
import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.application.billing.payment.port.out.PaymentPortOut;
import com.ban.vehicle_management.application.catalog.pricerule.port.out.PriceRulePortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.parking.parkingevent.port.out.ParkingEventPortOut;
import com.ban.vehicle_management.application.people.customer.port.out.CustomerPortOut;
import com.ban.vehicle_management.application.people.customervehicle.port.out.CustomerVehiclePortOut;
import com.ban.vehicle_management.application.storage.port.out.FileAccessPort;
import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.domain.accesscontrol.card.policy.CardPolicy;
import com.ban.vehicle_management.domain.accesscontrol.lostcardreport.model.LostCardReport;
import com.ban.vehicle_management.domain.accesscontrol.lostcardreport.policy.LostCardReportPolicy;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.billing.invoice.model.InvoiceDetail;
import com.ban.vehicle_management.domain.billing.invoice.policy.InvoicePolicy;
import com.ban.vehicle_management.domain.catalog.pricerule.model.PriceRule;
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.domain.parking.parkingevent.model.ParkingEvent;
import com.ban.vehicle_management.domain.parking.parkingsession.policy.ParkingCheckoutPricePolicy;
import com.ban.vehicle_management.domain.parking.parkingsession.policy.ParkingLicensePlatePolicy;
import com.ban.vehicle_management.domain.parking.parkingsession.policy.ParkingSessionPolicy;
import com.ban.vehicle_management.domain.people.customervehicle.model.CustomerVehicle;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.CardStatus;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportContext;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportStatus;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingEventType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.DateTimeUtils;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LostCardReportUseCaseImpl implements LostCardReportPortIn {

    private static final String BARRIER_ACTION_OPEN = "OPEN";
    private static final String BARRIER_ACTION_NONE = "NONE";
    private static final int CHECK_IN_IMAGE_READ_URL_EXPIRE_SECONDS = 15 * 60;
    private static final LocalTime DAY_REFERENCE_TIME = LocalTime.NOON;
    private static final LocalTime NIGHT_REFERENCE_TIME = LocalTime.MIDNIGHT;

    private static final List<InvoiceStatus> ACTIVE_INVOICE_STATUSES = List.of(
            InvoiceStatus.UNPAID,
            InvoiceStatus.PAID
    );

    private static final DateTimeFormatter INVOICE_NO_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyyMMddHHmmss")
            .withZone(DateTimeUtils.VIETNAM_ZONE);

    private final CurrentAccountPortIn currentAccountPortIn;
    private final LostCardReportAccessGuard lostCardReportAccessGuard;
    private final LostCardReportPortOut lostCardReportPortOut;
    private final ParkingSessionPortOut parkingSessionPortOut;
    private final ParkingEventPortOut parkingEventPortOut;
    private final SubscriptionPortOut subscriptionPortOut;
    private final CardPortOut cardPortOut;
    private final PriceRulePortOut priceRulePortOut;
    private final InvoicePortOut invoicePortOut;
    private final PaymentPortOut paymentPortOut;
    private final CustomerPortOut customerPortOut;
    private final CustomerVehiclePortOut customerVehiclePortOut;
    private final FileAccessPort fileAccessPort;

    private final LostCardReportPolicy lostCardReportPolicy = new LostCardReportPolicy();
    private final ParkingSessionPolicy parkingSessionPolicy = new ParkingSessionPolicy();
    private final ParkingCheckoutPricePolicy parkingCheckoutPricePolicy = new ParkingCheckoutPricePolicy();
    private final ParkingLicensePlatePolicy licensePlatePolicy = new ParkingLicensePlatePolicy();
    private final CardPolicy cardPolicy = new CardPolicy();
    private final InvoicePolicy invoicePolicy = new InvoicePolicy();

    public LostCardReportUseCaseImpl(
            CurrentAccountPortIn currentAccountPortIn,
            LostCardReportAccessGuard lostCardReportAccessGuard,
            LostCardReportPortOut lostCardReportPortOut,
            ParkingSessionPortOut parkingSessionPortOut,
            ParkingEventPortOut parkingEventPortOut,
            SubscriptionPortOut subscriptionPortOut,
            CardPortOut cardPortOut,
            PriceRulePortOut priceRulePortOut,
            InvoicePortOut invoicePortOut,
            PaymentPortOut paymentPortOut,
            CustomerPortOut customerPortOut,
            CustomerVehiclePortOut customerVehiclePortOut,
            FileAccessPort fileAccessPort
    ) {
        this.currentAccountPortIn = currentAccountPortIn;
        this.lostCardReportAccessGuard = lostCardReportAccessGuard;
        this.lostCardReportPortOut = lostCardReportPortOut;
        this.parkingSessionPortOut = parkingSessionPortOut;
        this.parkingEventPortOut = parkingEventPortOut;
        this.subscriptionPortOut = subscriptionPortOut;
        this.cardPortOut = cardPortOut;
        this.priceRulePortOut = priceRulePortOut;
        this.invoicePortOut = invoicePortOut;
        this.paymentPortOut = paymentPortOut;
        this.customerPortOut = customerPortOut;
        this.customerVehiclePortOut = customerVehiclePortOut;
        this.fileAccessPort = fileAccessPort;
    }

    @Override
    @Transactional(readOnly = true)
    public LostCardPreviewResult previewByLicensePlate(String licensePlate) {
        lostCardReportAccessGuard.ensureCanRead();

        String normalizedLicensePlate = licensePlatePolicy.normalizeRequired(licensePlate, "licensePlate");
        Instant now = Instant.now();
        LocalDate businessDate = DateTimeUtils.toVietnamLocalDate(now);

        List<ParkingSession> openSessions = parkingSessionPortOut.findOpenByLicensePlateIn(normalizedLicensePlate);
        if (openSessions.size() > 1) {
            throw new ConflictException("Multiple open parking sessions found for license plate");
        }

        if (openSessions.size() == 1) {
            ParkingSession session = openSessions.getFirst();
            LostCardReportContext context = isRegisteredSession(session)
                    ? LostCardReportContext.REGISTERED_IN_PARKING
                    : LostCardReportContext.VISITOR_IN_PARKING;

            Subscription subscription = context == LostCardReportContext.REGISTERED_IN_PARKING
                    ? findActiveSubscriptionBySession(session, businessDate)
                    : null;

            BigDecimal ticketPrice = context == LostCardReportContext.VISITOR_IN_PARKING
                    ? calculateVisitorPrice(session, now)
                    : BigDecimal.ZERO;
            BigDecimal lostCardFee = resolveLostCardFee(session, subscription, businessDate, now);
            ParkingEvent checkInEvent = parkingEventPortOut
                    .findLatestBySessionIdAndEventType(session.getParkingSessionId(), ParkingEventType.CHECK_IN)
                    .orElse(null);

            return new LostCardPreviewResult(
                    context,
                    session,
                    subscription,
                    session.getCardId(),
                    session.getCustomerId(),
                    session.getCustomerVehicleId(),
                    ticketPrice,
                    lostCardFee,
                    ticketPrice.add(lostCardFee),
                    resolveCardNumber(session.getCardId()),
                    resolveCustomerName(context == LostCardReportContext.REGISTERED_IN_PARKING
                            ? subscription.getCustomerId()
                            : session.getCustomerId()),
                    session.getLicensePlateIn(),
                    resolvePrivateReadUrl(checkInEvent == null ? null : checkInEvent.getLicensePlateImagePath()),
                    resolvePrivateReadUrl(checkInEvent == null ? null : checkInEvent.getPersonImagePath())
            );
        }

        Subscription subscription = subscriptionPortOut.findActiveByLicensePlate(normalizedLicensePlate, businessDate)
                .orElseThrow(() -> new NotFoundException("Open parking session or active subscription not found"));

        ParkingSession session = null;
        BigDecimal lostCardFee = resolveLostCardFee(session, subscription, businessDate, now);

        return new LostCardPreviewResult(
                LostCardReportContext.REGISTERED_OUTSIDE,
                null,
                subscription,
                subscription.getCardId(),
                subscription.getCustomerId(),
                subscription.getCustomerVehicleId(),
                BigDecimal.ZERO,
                lostCardFee,
                lostCardFee,
                resolveCardNumber(subscription.getCardId()),
                resolveCustomerName(subscription.getCustomerId()),
                resolveLicensePlate(null, subscription),
                null,
                null
        );
    }

    @Override
    @Transactional
    public LostCardReportWorkflowResult createReport(LostCardReport report) {
        lostCardReportAccessGuard.ensureCanCreate();
        requireField(report, "lostCardReport");

        Instant now = Instant.now();
        LocalDate businessDate = DateTimeUtils.toVietnamLocalDate(now);

        ParkingSession session = null;
        Subscription subscription = null;
        LostCardReportContext context;

        boolean hasParkingSessionId = report.getParkingSessionId() != null;
        boolean hasSubscriptionId = report.getSubscriptionId() != null;

        if (!hasParkingSessionId && !hasSubscriptionId) {
            throw new BadRequestException("parkingSessionId or subscriptionId must be provided");
        }

        if (hasParkingSessionId) {
            session = findParkingSession(report.getParkingSessionId());
            requireParkingSessionOpen(session);

            context = isRegisteredSession(session)
                    ? LostCardReportContext.REGISTERED_IN_PARKING
                    : LostCardReportContext.VISITOR_IN_PARKING;

            if (context == LostCardReportContext.REGISTERED_IN_PARKING) {
                subscription = findActiveSubscriptionBySession(session, businessDate);
                if (hasSubscriptionId && !subscription.getSubscriptionId().equals(report.getSubscriptionId())) {
                    throw new ConflictException("subscriptionId does not match parking session");
                }
            } else if (hasSubscriptionId) {
                throw new ConflictException("Visitor lost card report must not contain subscriptionId");
            }
        } else {
            subscription = findSubscription(report.getSubscriptionId());
            requireSubscriptionActive(subscription, businessDate);
            context = LostCardReportContext.REGISTERED_OUTSIDE;
        }

        if (session != null
                && report.getTimeOfLost() != null
                && report.getTimeOfLost().isBefore(session.getCheckInTime())) {
            throw new BadRequestException("Thời gian mất thẻ không được trước thời gian check-in.");
        }

        UUID cardId = context == LostCardReportContext.REGISTERED_OUTSIDE
                ? subscription.getCardId()
                : session.getCardId();
        Card oldCard = findCard(cardId);

        if (lostCardReportPortOut.existsOpenByCardId(cardId)) {
            throw new ConflictException("Open lost card report already exists for card");
        }
        if (session != null && lostCardReportPortOut.existsOpenByParkingSessionId(session.getParkingSessionId())) {
            throw new ConflictException("Open lost card report already exists for parking session");
        }

        BigDecimal ticketPrice = context == LostCardReportContext.VISITOR_IN_PARKING
                ? calculateVisitorPrice(session, now)
                : BigDecimal.ZERO;
        BigDecimal lostCardFee = resolveLostCardFee(session, subscription, businessDate, now);

        report.setLostCardReportId(UUID.randomUUID());
        report.setCardId(cardId);
        report.setCustomerId(context == LostCardReportContext.VISITOR_IN_PARKING ? null : subscription.getCustomerId());
        report.setParkingSessionId(session == null ? null : session.getParkingSessionId());
        report.setSubscriptionId(subscription == null ? null : subscription.getSubscriptionId());
        report.setNotificationTime(now);
        report.setTicketPrice(ticketPrice);
        report.setLostCardFee(lostCardFee);
        report.setContext(context);

        lostCardReportPolicy.initializeNewReport(report);

        if (session != null) {
            parkingSessionPolicy.markLostCard(session, now, ticketPrice);
            session = parkingSessionPortOut.save(session);
        }

        cardPolicy.markLost(oldCard);
        cardPortOut.save(oldCard);

        LostCardReport savedReport = lostCardReportPortOut.save(report);
        Invoice invoice = createLostCardInvoice(savedReport, ticketPrice.add(lostCardFee), now);

        return new LostCardReportWorkflowResult(
                savedReport,
                session,
                subscription,
                invoice,
                BARRIER_ACTION_NONE
        );
    }

    @Override
    @Transactional
    public LostCardReportWorkflowResult resolveReport(UUID lostCardReportId, UUID newCardId) {
        lostCardReportAccessGuard.ensureCanUpdate();

        LostCardReport report = findReport(lostCardReportId);
        if (report.getStatus() != LostCardReportStatus.OPEN) {
            throw new ConflictException("Only open lost card report can be resolved");
        }

        Invoice invoice = findInvoiceForReport(report.getLostCardReportId());
        if (invoice.getStatus() != InvoiceStatus.PAID) {
            throw new ConflictException("Lost card invoice must be paid before resolving report");
        }

        ParkingSession session = report.getParkingSessionId() == null
                ? null
                : findParkingSession(report.getParkingSessionId());
        Subscription subscription = report.getSubscriptionId() == null
                ? null
                : findSubscription(report.getSubscriptionId());

        if (report.getContext() == LostCardReportContext.VISITOR_IN_PARKING) {
            if (newCardId != null) {
                throw new BadRequestException("Visitor lost card report must not contain newCardId");
            }
            session = closeLostCardSession(session);
        } else {
            requireField(newCardId, "newCardId");
            requireField(subscription, "subscription");

            Card newCard = findCard(newCardId);
            if (newCard.getStatus() != CardStatus.AVAILABLE) {
                throw new ConflictException("New card must be AVAILABLE");
            }

            Card oldCard = findCard(report.getCardId());
            if (!Objects.equals(oldCard.getCardTypeId(), newCard.getCardTypeId())) {
                throw new ConflictException("New card must have the same card type as the lost card");
            }

            cardPolicy.assign(newCard, Instant.now());
            cardPortOut.save(newCard);

            subscription.setCardId(newCardId);
            subscription = subscriptionPortOut.save(subscription);

            if (session != null) {
                session = closeLostCardSession(session);
            }
        }

        UUID currentAccountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        Instant now = Instant.now();
        lostCardReportPolicy.resolve(report, currentAccountId, now);
        LostCardReport savedReport = lostCardReportPortOut.save(report);

        return new LostCardReportWorkflowResult(
                savedReport,
                session,
                subscription,
                invoice,
                report.getContext() == LostCardReportContext.REGISTERED_OUTSIDE ? BARRIER_ACTION_NONE : BARRIER_ACTION_OPEN
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<LostCardReplacementCardResult> getAvailableReplacementCards(UUID lostCardReportId) {
        lostCardReportAccessGuard.ensureCanUpdate();

        LostCardReport report = findReport(lostCardReportId);
        if (report.getStatus() != LostCardReportStatus.OPEN
                || report.getContext() == LostCardReportContext.VISITOR_IN_PARKING) {
            return List.of();
        }

        Card oldCard = findCard(report.getCardId());
        return cardPortOut.findAll(CardStatus.AVAILABLE, oldCard.getCardTypeId(), null)
                .stream()
                .map(card -> new LostCardReplacementCardResult(
                        card.getCardId(),
                        card.getCardNumber(),
                        card.getUid(),
                        card.getCardTypeId(),
                        card.getStatus()
                ))
                .toList();
    }

    @Override
    @Transactional
    public LostCardReportWorkflowResult cancelReport(UUID lostCardReportId, String cancelReason) {
        lostCardReportAccessGuard.ensureCanUpdate();

        LostCardReport report = findReport(lostCardReportId);
        if (report.getStatus() != LostCardReportStatus.OPEN) {
            throw new ConflictException("Only open lost card report can be cancelled");
        }

        Invoice invoice = findInvoiceForReport(report.getLostCardReportId());
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new ConflictException("Paid lost card report cannot be cancelled");
        }

        if (paymentPortOut.existsByInvoiceIdAndStatus(invoice.getInvoiceId(), PaymentStatus.SUCCESS)) {
            throw new ConflictException("Lost card report has successful payment and cannot be cancelled");
        }

        invoicePolicy.cancel(invoice);
        invoice = invoicePortOut.save(invoice);

        ParkingSession session = report.getParkingSessionId() == null
                ? null
                : findParkingSession(report.getParkingSessionId());
        Subscription subscription = report.getSubscriptionId() == null
                ? null
                : findSubscription(report.getSubscriptionId());

        Card oldCard = findCard(report.getCardId());
        restoreOldCard(oldCard, report.getContext());
        cardPortOut.save(oldCard);

        if (session != null && session.getStatus() == ParkingSessionStatus.LOST_CARD) {
            session.setStatus(ParkingSessionStatus.OPEN);
            session.setCheckOutTime(null);
            session.setTotalPrice(null);
            session.setLicensePlateOut(null);
            session = parkingSessionPortOut.save(session);
        }

        UUID currentAccountId = currentAccountPortIn.getCurrentAccountIdOrThrow();
        Instant now = Instant.now();
        lostCardReportPolicy.cancel(report, currentAccountId, now, cancelReason);
        LostCardReport savedReport = lostCardReportPortOut.save(report);

        return new LostCardReportWorkflowResult(
                savedReport,
                session,
                subscription,
                invoice,
                BARRIER_ACTION_NONE
        );
    }

    @Override
    @Transactional(readOnly = true)
    public LostCardReportDetailResult getReportById(UUID lostCardReportId) {
        lostCardReportAccessGuard.ensureCanRead();

        LostCardReport report = findReport(lostCardReportId);
        ParkingSession session = report.getParkingSessionId() == null
                ? null
                : findParkingSession(report.getParkingSessionId());
        Subscription subscription = report.getSubscriptionId() == null
                ? null
                : findSubscription(report.getSubscriptionId());
        Invoice invoice = findInvoiceForReport(report.getLostCardReportId());
        ParkingEvent checkInEvent = session == null
                ? null
                : parkingEventPortOut
                .findLatestBySessionIdAndEventType(session.getParkingSessionId(), ParkingEventType.CHECK_IN)
                .orElse(null);

        return new LostCardReportDetailResult(
                report,
                resolveCardNumber(report.getCardId()),
                resolveCustomerName(report.getCustomerId()),
                resolveLicensePlate(session, subscription),
                session,
                subscription,
                new InvoiceDetail(invoice, paymentPortOut.findByInvoiceId(invoice.getInvoiceId())),
                resolvePrivateReadUrl(checkInEvent == null ? null : checkInEvent.getLicensePlateImagePath()),
                resolvePrivateReadUrl(checkInEvent == null ? null : checkInEvent.getPersonImagePath())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<LostCardReport> getReports(
            LostCardReportStatus status,
            LostCardReportContext context,
            UUID customerId,
            UUID cardId,
            UUID parkingSessionId,
            UUID subscriptionId,
            Instant fromDate,
            Instant toDate,
            String keyword
    ) {
        lostCardReportAccessGuard.ensureCanRead();

        return lostCardReportPortOut.findAll(
                status,
                context,
                customerId,
                cardId,
                parkingSessionId,
                subscriptionId,
                fromDate,
                toDate,
                normalizeKeyword(keyword)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<LostCardReportListItemResult> getReportListItems(
            LostCardReportStatus status,
            LostCardReportContext context,
            UUID customerId,
            UUID cardId,
            UUID parkingSessionId,
            UUID subscriptionId,
            Instant fromDate,
            Instant toDate,
            String keyword
    ) {
        lostCardReportAccessGuard.ensureCanRead();

        return lostCardReportPortOut.findListItems(
                status,
                context,
                customerId,
                cardId,
                parkingSessionId,
                subscriptionId,
                fromDate,
                toDate,
                normalizeKeyword(keyword)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public LostCardReportSummaryResult getSummary(Instant fromDate, Instant toDate) {
        lostCardReportAccessGuard.ensureCanRead();

        return new LostCardReportSummaryResult(
                lostCardReportPortOut.countByStatus(LostCardReportStatus.OPEN),
                lostCardReportPortOut.countOpenByInvoiceStatus(InvoiceStatus.UNPAID),
                lostCardReportPortOut.countByStatusAndResolvedAtBetween(LostCardReportStatus.RESOLVED, fromDate, toDate),
                lostCardReportPortOut.countDistinctCardsByCardStatus(CardStatus.LOST)
        );
    }

    private BigDecimal calculateVisitorPrice(ParkingSession session, Instant referenceTime) {
        LocalDate effectiveDate = DateTimeUtils.toVietnamLocalDate(referenceTime);
        PriceRule dayRule = priceRulePortOut.findActiveVisitorRuleByTime(
                        session.getVehicleTypeId(),
                        effectiveDate,
                        DAY_REFERENCE_TIME
                )
                .orElseThrow(() -> new NotFoundException("Active day parking price rule not found"));
        PriceRule nightRule = priceRulePortOut.findActiveVisitorRuleByTime(
                        session.getVehicleTypeId(),
                        effectiveDate,
                        NIGHT_REFERENCE_TIME
                )
                .orElseThrow(() -> new NotFoundException("Active night parking price rule not found"));

        LocalDateTime checkInLocalTime = LocalDateTime.ofInstant(session.getCheckInTime(), DateTimeUtils.VIETNAM_ZONE);
        LocalDateTime checkOutLocalTime = LocalDateTime.ofInstant(referenceTime, DateTimeUtils.VIETNAM_ZONE);

        return parkingCheckoutPricePolicy.calculateVisitorPrice(
                checkInLocalTime,
                checkOutLocalTime,
                dayRule.getBasePrice(),
                nightRule.getBasePrice(),
                dayRule.getTimeFrom(),
                dayRule.getTimeTo()
        );
    }

    private BigDecimal resolveLostCardFee(
            ParkingSession session,
            Subscription subscription,
            LocalDate businessDate,
            Instant referenceTime
    ) {
        PriceRule priceRule;

        if (subscription != null) {
            priceRule = priceRulePortOut.findById(subscription.getPriceRuleId())
                    .orElseThrow(() -> new NotFoundException("Subscription price rule not found"));
        } else {
            priceRule = priceRulePortOut.findActiveVisitorRuleByTime(
                            session.getVehicleTypeId(),
                            businessDate,
                            LocalDateTime.ofInstant(referenceTime, DateTimeUtils.VIETNAM_ZONE).toLocalTime()
                    )
                    .orElseThrow(() -> new NotFoundException("Active parking price rule not found"));
        }

        return priceRule.getLostCardFee() == null ? BigDecimal.ZERO : priceRule.getLostCardFee();
    }

    private Invoice createLostCardInvoice(LostCardReport report, BigDecimal totalAmount, Instant issuedAt) {
        if (invoicePortOut.existsByLostCardReportIdAndStatusIn(
                report.getLostCardReportId(),
                ACTIVE_INVOICE_STATUSES
        )) {
            throw new ConflictException("Active invoice already exists for lost card report");
        }

        Invoice invoice = new Invoice();
        invoice.setInvoiceId(UUID.randomUUID());
        invoice.setCustomerId(report.getCustomerId());
        invoice.setLostCardReportId(report.getLostCardReportId());
        invoice.setAmount(totalAmount);
        invoice.setDiscountAmount(BigDecimal.ZERO);

        invoicePolicy.initializeNewInvoice(invoice, generateInvoiceNo(invoice.getInvoiceId(), issuedAt), issuedAt);
        return invoicePortOut.save(invoice);
    }

    private ParkingSession closeLostCardSession(ParkingSession session) {
        requireField(session, "parkingSession");

        if (session.getStatus() != ParkingSessionStatus.LOST_CARD) {
            throw new ConflictException("Parking session must be LOST_CARD to resolve lost card report");
        }

        session.setStatus(ParkingSessionStatus.CLOSED);
        if (session.getTotalPrice() == null) {
            session.setTotalPrice(BigDecimal.ZERO);
        }
        return parkingSessionPortOut.save(session);
    }

    private void restoreOldCard(Card oldCard, LostCardReportContext context) {
        if (oldCard.getStatus() != CardStatus.LOST) {
            throw new ConflictException("Lost card can only be restored from LOST status");
        }

        if (context == LostCardReportContext.REGISTERED_OUTSIDE) {
            oldCard.setStatus(CardStatus.ASSIGNED);
        } else {
            oldCard.setStatus(CardStatus.IN_USE);
        }
    }

    private Subscription findActiveSubscriptionBySession(ParkingSession session, LocalDate businessDate) {
        return subscriptionPortOut.findActiveByCardId(session.getCardId(), businessDate)
                .orElseThrow(() -> new ConflictException("Active subscription not found for card"));
    }

    private Subscription findSubscription(UUID subscriptionId) {
        return subscriptionPortOut.findById(subscriptionId)
                .orElseThrow(() -> new NotFoundException("Subscription not found"));
    }

    private void requireSubscriptionActive(Subscription subscription, LocalDate businessDate) {
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE
                || businessDate.isBefore(subscription.getEffectiveFrom())
                || businessDate.isAfter(subscription.getEffectiveTo())) {
            throw new ConflictException("Subscription is not active");
        }
    }

    private ParkingSession findParkingSession(UUID parkingSessionId) {
        return parkingSessionPortOut.findById(parkingSessionId)
                .orElseThrow(() -> new NotFoundException("Parking session not found"));
    }

    private Card findCard(UUID cardId) {
        return cardPortOut.findById(cardId)
                .orElseThrow(() -> new NotFoundException("Card not found"));
    }

    private LostCardReport findReport(UUID lostCardReportId) {
        return lostCardReportPortOut.findById(lostCardReportId)
                .orElseThrow(() -> new NotFoundException("Lost card report not found"));
    }

    private Invoice findInvoiceForReport(UUID lostCardReportId) {
        return invoicePortOut.findAll(
                        null,
                        null,
                        null,
                        lostCardReportId,
                        null,
                        null,
                        null,
                        null
                )
                .stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Lost card invoice not found"));
    }

    private void requireParkingSessionOpen(ParkingSession session) {
        if (session.getStatus() != ParkingSessionStatus.OPEN) {
            throw new ConflictException("Parking session must be OPEN");
        }
    }

    private boolean isRegisteredSession(ParkingSession session) {
        return session.getCustomerId() != null || session.getCustomerVehicleId() != null;
    }

    private String resolvePrivateReadUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank() || isBrowserReachableUrl(objectKey)) {
            return objectKey;
        }
        return fileAccessPort.createReadUrl(objectKey, CHECK_IN_IMAGE_READ_URL_EXPIRE_SECONDS);
    }

    private boolean isBrowserReachableUrl(String value) {
        String normalized = value.toLowerCase();
        return normalized.startsWith("http://")
                || normalized.startsWith("https://")
                || normalized.startsWith("/");
    }

    private UUID resolveRegisteredVehicleType(ParkingSession session, Subscription subscription) {
        if (session != null) {
            return session.getVehicleTypeId();
        }

        CustomerVehicle customerVehicle = customerVehiclePortOut.findById(subscription.getCustomerVehicleId())
                .orElseThrow(() -> new NotFoundException("Customer vehicle not found"));

        return customerVehicle.getVehicleTypeId();
    }

    private String resolveLicensePlate(ParkingSession session, Subscription subscription) {
        if (session != null) {
            return session.getLicensePlateIn();
        }
        if (subscription == null || subscription.getCustomerVehicleId() == null) {
            return null;
        }

        return customerVehiclePortOut.findById(subscription.getCustomerVehicleId())
                .map(CustomerVehicle::getLicensePlate)
                .orElse(null);
    }

    private String resolveCardNumber(UUID cardId) {
        if (cardId == null) {
            return null;
        }

        return cardPortOut.findById(cardId)
                .map(Card::getCardNumber)
                .orElse(null);
    }

    private String resolveCustomerName(UUID customerId) {
        if (customerId == null) {
            return null;
        }

        return customerPortOut.findById(customerId)
                .map(customer -> {
                    if (customer.getUserProfile() != null
                            && customer.getUserProfile().getFullName() != null
                            && !customer.getUserProfile().getFullName().isBlank()) {
                        return customer.getUserProfile().getFullName();
                    }
                    return customer.getCustomerCode();
                })
                .orElse(null);
    }

    private String generateInvoiceNo(UUID invoiceId, Instant now) {
        String suffix = invoiceId.toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();
        return "INV-" + INVOICE_NO_TIME_FORMATTER.format(now) + "-" + suffix;
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }
}
