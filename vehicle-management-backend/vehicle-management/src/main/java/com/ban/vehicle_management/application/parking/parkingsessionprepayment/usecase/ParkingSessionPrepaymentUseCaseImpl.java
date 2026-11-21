package com.ban.vehicle_management.application.parking.parkingsessionprepayment.usecase;

import com.ban.vehicle_management.application.accesscontrol.card.port.out.CardPortOut;
import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.application.parking.parkinglot.port.out.ParkingLotPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.service.ParkingSessionChargeService;
import com.ban.vehicle_management.application.parking.parkingsessionprepayment.model.ParkingSessionPrepaymentView;
import com.ban.vehicle_management.application.parking.parkingsessionprepayment.port.in.ParkingSessionPrepaymentPortIn;
import com.ban.vehicle_management.application.parking.parkingsessionprepayment.port.out.ParkingSessionPrepaymentPortOut;
import com.ban.vehicle_management.domain.accesscontrol.card.model.Card;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.billing.invoice.policy.InvoicePolicy;
import com.ban.vehicle_management.domain.common.licenseplate.LicensePlatePolicy;
import com.ban.vehicle_management.domain.common.licenseplate.LicensePlateResolution;
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.domain.parking.parkingsession.policy.ParkingLicensePlatePolicy;
import com.ban.vehicle_management.domain.parking.parkingsessionprepayment.model.ParkingSessionPrepayment;
import com.ban.vehicle_management.shared.enumeration.billing.InvoicePurpose;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionPrepaymentStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ParkingSessionPrepaymentUseCaseImpl implements ParkingSessionPrepaymentPortIn {
    private static final int MIN_ESTIMATED_MINUTES = 5;
    private static final int MAX_ESTIMATED_MINUTES = 240;
    private static final Duration PAYMENT_WINDOW = Duration.ofMinutes(10);
    private static final List<InvoiceStatus> ACTIVE_CHECKOUT_STATUSES = List.of(InvoiceStatus.UNPAID);
    private static final DateTimeFormatter INVOICE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    private final CardPortOut cards;
    private final ParkingSessionPortOut sessions;
    private final ParkingSessionPrepaymentPortOut prepayments;
    private final ParkingSessionChargeService charges;
    private final InvoicePortOut invoices;
    private final ParkingLotPortOut parkingLots;
    private final ParkingLicensePlatePolicy licensePlatePolicy = new ParkingLicensePlatePolicy();
    private final LicensePlatePolicy licensePlateSnapshotPolicy = new LicensePlatePolicy();
    private final InvoicePolicy invoicePolicy = new InvoicePolicy();

    public ParkingSessionPrepaymentUseCaseImpl(
            CardPortOut cards,
            ParkingSessionPortOut sessions,
            ParkingSessionPrepaymentPortOut prepayments,
            ParkingSessionChargeService charges,
            InvoicePortOut invoices,
            ParkingLotPortOut parkingLots
    ) {
        this.cards = cards;
        this.sessions = sessions;
        this.prepayments = prepayments;
        this.charges = charges;
        this.invoices = invoices;
        this.parkingLots = parkingLots;
    }

    @Override
    @Transactional(readOnly = true)
    public ParkingSessionPrepaymentView lookup(String licensePlate, String cardNumber) {
        AccessContext context = resolveAccess(licensePlate, cardNumber, false);
        ParkingSessionPrepayment latest = prepayments.findBySessionAndStatus(
                        context.session().getParkingSessionId(), ParkingSessionPrepaymentStatus.PAID)
                .stream().findFirst().orElse(null);
        return latest == null
                ? viewWithoutQuote(context)
                : toView(latest, context);
    }

    @Override
    @Transactional
    public ParkingSessionPrepaymentView create(
            String licensePlate,
            String cardNumber,
            int estimatedMinutes,
            boolean policyAccepted
    ) {
        if (!policyAccepted) {
            throw new BadRequestException("You must accept the prepayment and no-refund policy");
        }
        if (estimatedMinutes < MIN_ESTIMATED_MINUTES || estimatedMinutes > MAX_ESTIMATED_MINUTES) {
            throw new BadRequestException("estimatedMinutes must be between 5 and 240");
        }

        AccessContext context = resolveAccess(licensePlate, cardNumber, true);
        ParkingSession session = context.session();
        if (session.getSubscriptionId() != null) {
            throw new ConflictException("Subscription parking sessions do not require prepayment");
        }
        expirePending(session.getParkingSessionId(), Instant.now());
        if (!prepayments.findBySessionAndStatus(
                session.getParkingSessionId(), ParkingSessionPrepaymentStatus.PENDING_PAYMENT).isEmpty()) {
            throw new ConflictException("This parking session already has a pending prepayment");
        }
        if (invoices.existsByParkingSessionIdAndPurposeAndStatusIn(
                session.getParkingSessionId(), InvoicePurpose.PARKING_SESSION,
                ACTIVE_CHECKOUT_STATUSES)) {
            throw new ConflictException("Checkout payment has already been prepared for this parking session");
        }

        Instant createdAt = Instant.now();
        Instant estimatedCheckOutAt = createdAt.plus(Duration.ofMinutes(estimatedMinutes));
        BigDecimal gross = charges.grossAmount(session, estimatedCheckOutAt);
        BigDecimal paidBefore = charges.successfulPrepaidAmount(session);
        BigDecimal amount = gross.subtract(paidBefore).max(BigDecimal.ZERO);
        if (amount.signum() == 0) {
            throw new ConflictException("The projected parking fee is already fully covered");
        }

        Invoice invoice = createInvoice(session, amount, createdAt);
        ParkingSessionPrepayment prepayment = new ParkingSessionPrepayment();
        prepayment.setPrepaymentId(UUID.randomUUID());
        prepayment.setParkingSessionId(session.getParkingSessionId());
        prepayment.setInvoiceId(invoice.getInvoiceId());
        prepayment.setEstimatedCheckOutAt(estimatedCheckOutAt);
        prepayment.setProjectedGrossAmount(gross);
        prepayment.setPaidBeforeAmount(paidBefore);
        prepayment.setAmount(amount);
        prepayment.setStatus(ParkingSessionPrepaymentStatus.PENDING_PAYMENT);
        prepayment.setExpiresAt(createdAt.plus(PAYMENT_WINDOW));
        prepayment.setPolicyVersion("1");
        prepayment.setCreatedAt(createdAt);
        return toView(prepayments.save(prepayment), context);
    }

    @Override
    @Transactional
    public ParkingSessionPrepaymentView get(UUID prepaymentId, String licensePlate, String cardNumber) {
        AccessContext context = resolveAccess(licensePlate, cardNumber, false);
        ParkingSessionPrepayment candidate = prepayments.findById(prepaymentId)
                .orElseThrow(() -> new NotFoundException("Parking session prepayment not found"));
        ensureSameSession(candidate, context.session());
        Invoice invoice = invoices.findByIdForUpdate(candidate.getInvoiceId())
                .orElseThrow(() -> new NotFoundException("Invoice not found"));
        ParkingSessionPrepayment prepayment = prepayments.findByIdForUpdate(prepaymentId)
                .orElseThrow(() -> new NotFoundException("Parking session prepayment not found"));
        ensureSameSession(prepayment, context.session());
        expire(prepayment, invoice, Instant.now());
        return toView(prepayment, context);
    }

    @Override
    @Transactional
    public UUID authorizePayment(UUID prepaymentId, String licensePlate, String cardNumber) {
        AccessContext context = resolveAccess(licensePlate, cardNumber, false);
        ParkingSessionPrepayment candidate = prepayments.findById(prepaymentId)
                .orElseThrow(() -> new NotFoundException("Parking session prepayment not found"));
        ensureSameSession(candidate, context.session());
        Invoice invoice = invoices.findByIdForUpdate(candidate.getInvoiceId())
                .orElseThrow(() -> new NotFoundException("Invoice not found"));
        ParkingSessionPrepayment prepayment = prepayments.findByIdForUpdate(prepaymentId)
                .orElseThrow(() -> new NotFoundException("Parking session prepayment not found"));
        ensureSameSession(prepayment, context.session());
        expire(prepayment, invoice, Instant.now());
        if (!ParkingSessionPrepaymentStatus.PENDING_PAYMENT.equals(prepayment.getStatus())) {
            throw new ConflictException("Parking session prepayment is no longer payable");
        }
        return prepayment.getInvoiceId();
    }

    @Override
    @Transactional
    public void onPaymentConfirmed(UUID invoiceId, Instant paidAt) {
        ParkingSessionPrepayment prepayment = prepayments.findByInvoiceIdForUpdate(invoiceId)
                .orElseThrow(() -> new NotFoundException("Parking session prepayment not found"));
        if (ParkingSessionPrepaymentStatus.PAID.equals(prepayment.getStatus())) {
            return;
        }
        if (!ParkingSessionPrepaymentStatus.PENDING_PAYMENT.equals(prepayment.getStatus())
                || paidAt.isAfter(prepayment.getExpiresAt())) {
            throw new ConflictException("Parking session prepayment expired before payment");
        }
        prepayment.setStatus(ParkingSessionPrepaymentStatus.PAID);
        prepayment.setPaidAt(paidAt);
        prepayment.setUpdatedAt(Instant.now());
        prepayments.save(prepayment);
    }

    private AccessContext resolveAccess(String rawLicensePlate, String rawCardNumber, boolean forUpdate) {
        String licensePlate = licensePlatePolicy.normalizeRequired(rawLicensePlate, "licensePlate");
        String cardNumber = TextValidationUtils.normalizeRequiredText(rawCardNumber, "cardNumber", 50);
        Card card = (forUpdate ? cards.findByCardNumberForUpdate(cardNumber) : cards.findByCardNumber(cardNumber))
                .orElseThrow(() -> new NotFoundException("Open parking session not found"));
        ParkingSession session = (forUpdate
                ? sessions.findOpenByCardIdForUpdate(card.getCardId())
                : sessions.findOpenByCardId(card.getCardId()))
                .orElseThrow(() -> new NotFoundException("Open parking session not found"));
        if (!licensePlatePolicy.matches(session.getLicensePlateIn(), licensePlate)) {
            throw new NotFoundException("Open parking session not found");
        }
        return new AccessContext(session, card);
    }

    private void expirePending(UUID sessionId, Instant now) {
        prepayments.findBySessionAndStatus(sessionId, ParkingSessionPrepaymentStatus.PENDING_PAYMENT)
                .forEach(value -> {
                    Invoice invoice = invoices.findByIdForUpdate(value.getInvoiceId())
                            .orElseThrow(() -> new NotFoundException("Invoice not found"));
                    ParkingSessionPrepayment locked = prepayments.findByIdForUpdate(value.getPrepaymentId())
                            .orElseThrow(() -> new NotFoundException(
                                    "Parking session prepayment not found"));
                    expire(locked, invoice, now);
                });
    }

    private void expire(ParkingSessionPrepayment prepayment, Invoice invoice, Instant now) {
        if (!ParkingSessionPrepaymentStatus.PENDING_PAYMENT.equals(prepayment.getStatus())
                || prepayment.getExpiresAt().isAfter(now)) {
            return;
        }
        if (InvoiceStatus.UNPAID.equals(invoice.getStatus())) {
            invoicePolicy.cancel(invoice);
            invoices.save(invoice);
        }
        prepayment.setStatus(ParkingSessionPrepaymentStatus.EXPIRED);
        prepayment.setUpdatedAt(now);
        prepayments.save(prepayment);
    }

    private Invoice createInvoice(ParkingSession session, BigDecimal amount, Instant now) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(UUID.randomUUID());
        invoice.setParkingLotId(session.getParkingLotId());
        invoice.setCustomerId(session.getCustomerId());
        invoice.setParkingSessionId(session.getParkingSessionId());
        invoice.setPurpose(InvoicePurpose.PARKING_SESSION_PREPAYMENT);
        invoice.setAmount(amount);
        invoice.setDiscountAmount(BigDecimal.ZERO);
        LicensePlateResolution plate = licensePlateSnapshotPolicy.resolve(
                session.getLicensePlateIn(), null);
        invoice.setLicensePlateNormalizedSnapshot(plate.normalized());
        invoice.setLicensePlateDisplaySnapshot(plate.display());
        invoice.setLicensePlateFormatSnapshot(plate.format().name());
        invoice.setLicensePlateFormatVersion(LicensePlatePolicy.FORMAT_VERSION);
        invoicePolicy.initializeNewInvoice(invoice,
                "PSP-" + INVOICE_TIME.format(now) + "-" + invoice.getInvoiceId().toString().substring(0, 8),
                now);
        return invoices.save(invoice);
    }

    private ParkingSessionPrepaymentView viewWithoutQuote(AccessContext context) {
        return new ParkingSessionPrepaymentView(
                null, null, context.session().getParkingSessionId(),
                context.session().getLicensePlateIn(), context.card().getCardNumber(),
                parkingLotName(context.session()), context.session().getCheckInTime(),
                null, null, charges.successfulPrepaidAmount(context.session()), null,
                null, null, null);
    }

    private ParkingSessionPrepaymentView toView(
            ParkingSessionPrepayment value,
            AccessContext context
    ) {
        return new ParkingSessionPrepaymentView(
                value.getPrepaymentId(), value.getInvoiceId(), value.getParkingSessionId(),
                context.session().getLicensePlateIn(), context.card().getCardNumber(),
                parkingLotName(context.session()), context.session().getCheckInTime(),
                value.getEstimatedCheckOutAt(), value.getProjectedGrossAmount(),
                value.getPaidBeforeAmount(), value.getAmount(), value.getStatus(),
                value.getExpiresAt(), value.getPaidAt());
    }

    private String parkingLotName(ParkingSession session) {
        return parkingLots.findById(session.getParkingLotId())
                .map(value -> value.getName()).orElse(null);
    }

    private void ensureSameSession(ParkingSessionPrepayment prepayment, ParkingSession session) {
        if (!prepayment.getParkingSessionId().equals(session.getParkingSessionId())) {
            throw new NotFoundException("Parking session prepayment not found");
        }
    }

    private record AccessContext(ParkingSession session, Card card) {
    }
}
