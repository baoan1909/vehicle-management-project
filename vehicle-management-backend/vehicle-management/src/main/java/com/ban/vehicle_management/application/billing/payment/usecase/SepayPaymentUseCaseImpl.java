package com.ban.vehicle_management.application.billing.payment.usecase;

import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.application.billing.payment.authorization.PaymentAccessGuard;
import com.ban.vehicle_management.application.billing.payment.model.command.SepayWebhookCommand;
import com.ban.vehicle_management.application.billing.payment.model.result.SepayPaymentInstruction;
import com.ban.vehicle_management.application.billing.payment.model.result.SepayPaymentResult;
import com.ban.vehicle_management.application.billing.payment.model.result.SepayWebhookData;
import com.ban.vehicle_management.application.billing.payment.port.in.PaymentCompletionPortIn;
import com.ban.vehicle_management.application.billing.payment.port.in.SepayPaymentPortIn;
import com.ban.vehicle_management.application.billing.payment.port.out.PaymentPortOut;
import com.ban.vehicle_management.application.billing.payment.port.out.SepayGatewayPortOut;
import com.ban.vehicle_management.application.billing.payment.port.out.SepayQuotaPortOut;
import com.ban.vehicle_management.application.parking.customerreservation.port.in.CustomerReservationPaymentPortIn;
import com.ban.vehicle_management.application.parking.customerreservation.port.in.CustomerReservationPortIn;
import com.ban.vehicle_management.application.parking.parkingsession.port.in.ParkingCheckoutCompletionPortIn;
import com.ban.vehicle_management.application.parking.parkingsessionprepayment.port.in.ParkingSessionPrepaymentPortIn;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.billing.invoice.policy.InvoicePolicy;
import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import com.ban.vehicle_management.domain.billing.payment.policy.PaymentPolicy;
import com.ban.vehicle_management.shared.enumeration.billing.FundCustody;
import com.ban.vehicle_management.shared.enumeration.billing.InvoicePurpose;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentMethod;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;
import com.ban.vehicle_management.shared.enumeration.parking.ParkingSessionPrepaymentStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SepayPaymentUseCaseImpl implements SepayPaymentPortIn {
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final PaymentPortOut payments;
    private final InvoicePortOut invoices;
    private final SepayGatewayPortOut sepayGateway;
    private final SepayQuotaPortOut quota;
    private final CustomerReservationPortIn reservations;
    private final CustomerReservationPaymentPortIn reservationPayments;
    private final PaymentCompletionPortIn paymentCompletion;
    private final PaymentAccessGuard paymentAccessGuard;
    private final ParkingCheckoutCompletionPortIn parkingCheckoutCompletion;
    private ParkingSessionPrepaymentPortIn parkingSessionPrepayments;
    private final BigDecimal vnpayMinimumAmount;
    private final PaymentPolicy paymentPolicy = new PaymentPolicy();
    private final InvoicePolicy invoicePolicy = new InvoicePolicy();

    public SepayPaymentUseCaseImpl(
            PaymentPortOut payments,
            InvoicePortOut invoices,
            SepayGatewayPortOut sepayGateway,
            SepayQuotaPortOut quota,
            CustomerReservationPortIn reservations,
            CustomerReservationPaymentPortIn reservationPayments,
            PaymentCompletionPortIn paymentCompletion,
            PaymentAccessGuard paymentAccessGuard,
            ParkingCheckoutCompletionPortIn parkingCheckoutCompletion,
            @Value("${app.payment.vnpay.minimum-amount:10000}") BigDecimal vnpayMinimumAmount
    ) {
        this.payments = payments;
        this.invoices = invoices;
        this.sepayGateway = sepayGateway;
        this.quota = quota;
        this.reservations = reservations;
        this.reservationPayments = reservationPayments;
        this.paymentCompletion = paymentCompletion;
        this.paymentAccessGuard = paymentAccessGuard;
        this.parkingCheckoutCompletion = parkingCheckoutCompletion;
        this.vnpayMinimumAmount = vnpayMinimumAmount;
    }

    @Autowired
    public void setParkingSessionPrepayments(ParkingSessionPrepaymentPortIn parkingSessionPrepayments) {
        this.parkingSessionPrepayments = parkingSessionPrepayments;
    }

    @Override
    @Transactional
    public SepayPaymentResult createGuestReservationPayment(UUID reservationId, String accessToken) {
        UUID invoiceId = reservations.authorizeGuestPayment(reservationId, accessToken);
        return createPayment(findInvoiceForUpdate(invoiceId), InvoicePurpose.RESERVATION_PREPAYMENT);
    }

    @Override
    @Transactional
    public SepayPaymentResult createParkingSessionPrepaymentPayment(
            UUID prepaymentId,
            String licensePlate,
            String cardNumber
    ) {
        if (parkingSessionPrepayments == null) {
            throw new IllegalStateException("Parking-session prepayment is unavailable");
        }
        UUID invoiceId = parkingSessionPrepayments.authorizePayment(
                prepaymentId, licensePlate, cardNumber);
        return createPayment(findInvoiceForUpdate(invoiceId), InvoicePurpose.PARKING_SESSION_PREPAYMENT);
    }

    @Override
    @Transactional
    public SepayPaymentResult createParkingCheckoutPayment(UUID invoiceId) {
        paymentAccessGuard.requireCanCreateAndGetAccountId();
        Invoice invoice = findInvoiceForUpdate(invoiceId);
        paymentAccessGuard.ensureCanRecordPayment(invoice);
        return createPayment(invoice, InvoicePurpose.PARKING_SESSION);
    }

    @Override
    public byte[] downloadParkingSessionPrepaymentQr(UUID prepaymentId, String licensePlate, String cardNumber) {
        var prepayment = parkingSessionPrepayments.get(prepaymentId, licensePlate, cardNumber);
        if (prepayment.status() != ParkingSessionPrepaymentStatus.PENDING_PAYMENT) {
            throw new ConflictException("Parking session prepayment is no longer payable");
        }
        Payment payment = payments.findFirstByInvoiceIdAndStatus(prepayment.invoiceId(), PaymentStatus.PENDING)
                .filter(candidate -> PaymentMethod.SEPAY.equals(candidate.getPaymentMethod())
                        && paymentPolicy.isPendingWithinPaymentWindow(candidate, Instant.now()))
                .orElseThrow(() -> new ConflictException("SePay payment QR has expired"));
        return sepayGateway.downloadPaymentQr(payment.getTransactionRef(), payment.getAmount(), payment.getCreatedAt());
    }

    private Invoice findInvoiceForUpdate(UUID invoiceId) {
        return invoices.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found"));
    }

    private SepayPaymentResult createPayment(Invoice invoice, InvoicePurpose expectedPurpose) {
        validateInvoice(invoice, expectedPurpose);
        UUID invoiceId = invoice.getInvoiceId();
        if (payments.existsByInvoiceIdAndStatus(invoiceId, PaymentStatus.SUCCESS)) {
            throw new ConflictException("Successful payment already exists for this invoice");
        }
        Optional<Payment> pending = payments.findFirstByInvoiceIdAndStatus(
                invoiceId, PaymentStatus.PENDING);
        if (pending.isPresent() && paymentPolicy.isPendingWithinPaymentWindow(pending.get(), Instant.now())) {
            if (PaymentMethod.SEPAY.equals(pending.get().getPaymentMethod())) {
                return instructionFor(pending.get());
            }
            throw new ConflictException("Another online payment is still pending for this invoice");
        }

        Instant createdAt = Instant.now();
        YearMonth period = YearMonth.from(createdAt.atZone(VIETNAM_ZONE));
        if (!quota.tryAcquire(period, sepayGateway.monthlyTransactionLimit())) {
            throw new ConflictException(
                    "SePay monthly transaction limit has been reached; please try again next month");
        }

        UUID paymentId = UUID.randomUUID();
        String transactionRef = generateTransactionRef();
        SepayPaymentInstruction instruction = sepayGateway.createPaymentInstruction(
                transactionRef, invoice.getFinalAmount(), createdAt);

        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        paymentPolicy.initializePendingSepayPayment(
                payment,
                invoiceId,
                invoice.getFinalAmount(),
                transactionRef,
                createdAt,
                instruction.expiresAt()
        );
        payment.setFundCustody(FundCustody.PLATFORM);
        return result(payments.save(payment), instruction);
    }

    @Override
    @Transactional
    public void processWebhook(SepayWebhookCommand command) {
        SepayWebhookData webhook = sepayGateway.verifyWebhook(command);
        if (!webhook.authenticated()) {
            throw new BadRequestException("Invalid SePay webhook signature");
        }
        if (!StringUtils.hasText(webhook.transactionRef())
                || !webhook.transactionRef().toUpperCase(Locale.ROOT)
                        .startsWith(sepayGateway.transactionPrefix())) {
            return;
        }
        if (webhook.amount() == null
                || !StringUtils.hasText(webhook.providerTransactionNo())
                || webhook.paidAt() == null
                || !"in".equalsIgnoreCase(webhook.transferType())) {
            throw new BadRequestException("Invalid SePay webhook payload");
        }

        UUID invoiceId = payments.findInvoiceIdByTransactionRef(webhook.transactionRef())
                .orElseThrow(() -> new NotFoundException("SePay payment not found"));
        Invoice invoice = findInvoiceForUpdate(invoiceId);
        Payment payment = payments.findByTransactionRefForUpdate(webhook.transactionRef())
                .orElseThrow(() -> new NotFoundException("SePay payment not found"));
        if (!PaymentMethod.SEPAY.equals(payment.getPaymentMethod())) {
            throw new ConflictException("Payment method does not match SePay");
        }
        if (payment.getAmount().compareTo(webhook.amount()) != 0) {
            throw new BadRequestException("SePay payment amount does not match invoice");
        }
        if (PaymentStatus.SUCCESS.equals(payment.getStatus())) {
            return;
        }
        if (!PaymentStatus.PENDING.equals(payment.getStatus())) {
            throw new ConflictException("SePay payment is no longer pending");
        }
        if (payments.existsByPaymentMethodAndProviderTransactionNo(
                PaymentMethod.SEPAY, webhook.providerTransactionNo())) {
            throw new ConflictException("SePay transaction was already used");
        }

        if (!InvoiceStatus.UNPAID.equals(invoice.getStatus())) {
            throw new ConflictException("Invoice is no longer payable");
        }

        Instant paidAt = webhook.paidAt();
        paymentPolicy.markSepaySuccessful(
                payment, paidAt, webhook.providerTransactionNo(), webhook.bankCode());
        Payment savedPayment = payments.save(payment);
        invoicePolicy.markPaid(invoice, paidAt);
        invoices.save(invoice);
        if (InvoicePurpose.RESERVATION_PREPAYMENT.equals(invoice.getPurpose())) {
            reservationPayments.onPaymentConfirmed(invoice.getInvoiceId(), paidAt);
        } else if (InvoicePurpose.PARKING_SESSION_PREPAYMENT.equals(invoice.getPurpose())) {
            if (parkingSessionPrepayments == null) {
                throw new IllegalStateException("Parking-session prepayment is unavailable");
            }
            parkingSessionPrepayments.onPaymentConfirmed(invoice.getInvoiceId(), paidAt);
        }
        paymentCompletion.completePaymentTruth(savedPayment.getPaymentId());
        if (InvoicePurpose.PARKING_SESSION.equals(invoice.getPurpose())) {
            parkingCheckoutCompletion.completePaidCheckout(invoice.getInvoiceId());
        }
    }

    private void validateInvoice(Invoice invoice, InvoicePurpose expectedPurpose) {
        if (invoice.getPurpose() != expectedPurpose
                || (expectedPurpose == InvoicePurpose.PARKING_SESSION && invoice.getParkingSessionId() == null)) {
            throw new ConflictException("Invoice purpose does not match the requested SePay payment");
        }
        if (!InvoiceStatus.UNPAID.equals(invoice.getStatus())) {
            throw new ConflictException("Only unpaid invoice can be paid");
        }
        if (invoice.getFinalAmount() == null
                || invoice.getFinalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Invoice amount must be greater than zero");
        }
        if (expectedPurpose == InvoicePurpose.RESERVATION_PREPAYMENT
                && invoice.getFinalAmount().compareTo(vnpayMinimumAmount) >= 0) {
            throw new BadRequestException(
                    "SePay is only available for reservation payments below "
                            + vnpayMinimumAmount.toPlainString() + " VND");
        }
    }

    private SepayPaymentResult instructionFor(Payment payment) {
        SepayPaymentInstruction instruction = sepayGateway.createPaymentInstruction(
                payment.getTransactionRef(), payment.getAmount(), payment.getCreatedAt());
        return new SepayPaymentResult(
                payment.getPaymentId(), payment.getInvoiceId(), payment.getTransactionRef(),
                instruction.qrCodeUrl(), instruction.bankCode(), instruction.accountNumber(),
                instruction.accountHolder(), payment.getExpiresAt());
    }

    private String generateTransactionRef() {
        String prefix = sepayGateway.transactionPrefix();
        for (int attempt = 0; attempt < 20; attempt++) {
            String candidate = prefix + String.format(
                    Locale.ROOT, "%08d", ThreadLocalRandom.current().nextInt(100_000_000));
            if (payments.findByTransactionRef(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new ConflictException("Unable to allocate a unique SePay payment code");
    }

    private SepayPaymentResult result(Payment payment, SepayPaymentInstruction instruction) {
        return new SepayPaymentResult(
                payment.getPaymentId(), payment.getInvoiceId(), payment.getTransactionRef(),
                instruction.qrCodeUrl(), instruction.bankCode(), instruction.accountNumber(),
                instruction.accountHolder(), payment.getExpiresAt());
    }
}
