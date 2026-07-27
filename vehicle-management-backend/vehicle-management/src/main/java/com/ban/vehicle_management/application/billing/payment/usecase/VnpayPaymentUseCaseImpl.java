package com.ban.vehicle_management.application.billing.payment.usecase;

import com.ban.vehicle_management.application.accesscontrol.subscription.port.in.SubscriptionPortIn;
import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.application.billing.payment.authorization.PaymentAccessGuard;
import com.ban.vehicle_management.application.billing.payment.model.VnpayPaymentRequest;
import com.ban.vehicle_management.application.billing.payment.model.command.CreateVnpayPaymentCommand;
import com.ban.vehicle_management.application.billing.payment.model.command.VnpayCallbackCommand;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayCallbackData;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayIpnResult;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayPaymentLink;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayPaymentResult;
import com.ban.vehicle_management.application.billing.payment.model.result.VnpayReturnResult;
import com.ban.vehicle_management.application.billing.payment.port.in.VnpayPaymentPortIn;
import com.ban.vehicle_management.application.billing.payment.port.out.PaymentPortOut;
import com.ban.vehicle_management.application.billing.payment.port.out.VnpayGatewayPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.port.in.ParkingCheckoutCompletionPortIn;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.billing.invoice.policy.InvoicePolicy;
import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import com.ban.vehicle_management.domain.billing.payment.policy.PaymentPolicy;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;

@Service
public class VnpayPaymentUseCaseImpl implements VnpayPaymentPortIn {

    private final PaymentPortOut paymentPortOut;
    private final InvoicePortOut invoicePortOut;
    private final VnpayGatewayPortOut vnpayGatewayPortOut;
    private final PaymentAccessGuard paymentAccessGuard;
    private final SubscriptionPortIn subscriptionPortIn;
    private final ParkingCheckoutCompletionPortIn parkingCheckoutCompletionPortIn;
    private final BigDecimal vnpayMinimumAmount;
    private final PaymentPolicy paymentPolicy = new PaymentPolicy();
    private final InvoicePolicy invoicePolicy = new InvoicePolicy();

    public VnpayPaymentUseCaseImpl(
            PaymentPortOut paymentPortOut,
            InvoicePortOut invoicePortOut,
            VnpayGatewayPortOut vnpayGatewayPortOut,
            PaymentAccessGuard paymentAccessGuard,
            SubscriptionPortIn subscriptionPortIn,
            ParkingCheckoutCompletionPortIn parkingCheckoutCompletionPortIn,
            @Value("${app.payment.vnpay.minimum-amount:10000}") BigDecimal vnpayMinimumAmount
    ) {
        this.paymentPortOut = paymentPortOut;
        this.invoicePortOut = invoicePortOut;
        this.vnpayGatewayPortOut = vnpayGatewayPortOut;
        this.paymentAccessGuard = paymentAccessGuard;
        this.subscriptionPortIn = subscriptionPortIn;
        this.parkingCheckoutCompletionPortIn = parkingCheckoutCompletionPortIn;
        this.vnpayMinimumAmount = vnpayMinimumAmount;
    }

    @Override
    @Transactional
    public VnpayPaymentResult createPayment(UUID invoiceId, CreateVnpayPaymentCommand command) {
        if (command == null) {
            throw new BadRequestException("payment request must not be null");
        }

        Invoice invoice = invoicePortOut.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found"));
        paymentAccessGuard.ensureCanCreateVnpayPayment(invoice);
        validatePayableInvoice(invoice);
        validateVnpayAmount(invoice);

        if (paymentPortOut.existsByInvoiceIdAndStatus(invoiceId, PaymentStatus.SUCCESS)) {
            throw new ConflictException("Successful payment already exists for this invoice");
        }
        Optional<Payment> pendingPayment = paymentPortOut.findFirstByInvoiceIdAndStatus(
                invoiceId,
                PaymentStatus.PENDING
        );
        if (pendingPayment.isPresent()) {
            VnpayPaymentResult reusablePayment = reusePendingPayment(
                    invoice,
                    pendingPayment.get(),
                    command
            );
            if (reusablePayment != null) {
                return reusablePayment;
            }
        }

        UUID paymentId = UUID.randomUUID();
        String transactionRef = "VNP" + paymentId.toString().replace("-", "");
        Instant createdAt = Instant.now();
        VnpayPaymentLink paymentLink = vnpayGatewayPortOut.createPaymentLink(new VnpayPaymentRequest(
                transactionRef,
                invoice.getFinalAmount(),
                buildOrderInfo(invoice),
                command.clientIp(),
                normalizeBankCode(command.bankCode()),
                command.locale(),
                createdAt
        ));

        Payment payment = new Payment();
        payment.setPaymentId(paymentId);
        paymentPolicy.initializePendingVnpayPayment(
                payment,
                invoiceId,
                invoice.getFinalAmount(),
                transactionRef,
                createdAt,
                paymentLink.expiresAt()
        );
        Payment savedPayment = paymentPortOut.save(payment);

        return new VnpayPaymentResult(
                savedPayment.getPaymentId(),
                savedPayment.getInvoiceId(),
                savedPayment.getTransactionRef(),
                paymentLink.paymentUrl(),
                savedPayment.getExpiresAt()
        );
    }

    private VnpayPaymentResult reusePendingPayment(
            Invoice invoice,
            Payment payment,
            CreateVnpayPaymentCommand command
    ) {
        Instant now = Instant.now();
        if (payment.getCreatedAt() == null
                || payment.getExpiresAt() == null
                || !payment.getExpiresAt().isAfter(now)) {
            paymentPolicy.markVnpayFailed(
                    payment,
                    null,
                    "EXPIRED",
                    "EXPIRED",
                    null,
                    null
            );
            paymentPortOut.save(payment);
            return null;
        }

        VnpayPaymentLink paymentLink = vnpayGatewayPortOut.createPaymentLink(new VnpayPaymentRequest(
                payment.getTransactionRef(),
                payment.getAmount(),
                buildOrderInfo(invoice),
                command.clientIp(),
                normalizeBankCode(command.bankCode()),
                command.locale(),
                payment.getCreatedAt()
        ));

        return new VnpayPaymentResult(
                payment.getPaymentId(),
                payment.getInvoiceId(),
                payment.getTransactionRef(),
                paymentLink.paymentUrl(),
                payment.getExpiresAt()
        );
    }

    @Override
    @Transactional
    public VnpayIpnResult processIpn(VnpayCallbackCommand command) {
        VnpayCallbackData callback = verifyCallback(command);
        if (!callback.validSignature()) {
            return new VnpayIpnResult("97", "Invalid signature");
        }
        if (!StringUtils.hasText(callback.transactionRef())) {
            return new VnpayIpnResult("01", "Order not found");
        }

        Payment payment = paymentPortOut.findByTransactionRefForUpdate(callback.transactionRef())
                .orElse(null);
        if (payment == null) {
            return new VnpayIpnResult("01", "Order not found");
        }
        if (callback.amount() == null || payment.getAmount().compareTo(callback.amount()) != 0) {
            return new VnpayIpnResult("04", "Invalid amount");
        }
        if (!PaymentStatus.PENDING.equals(payment.getStatus())) {
            return new VnpayIpnResult("02", "Order already confirmed");
        }

        if (callback.isSuccessful()) {
            completeSuccessfulPayment(payment, callback);
        } else {
            paymentPolicy.markVnpayFailed(
                    payment,
                    callback.providerTransactionNo(),
                    callback.responseCode(),
                    callback.transactionStatus(),
                    callback.bankCode(),
                    callback.cardType()
            );
            paymentPortOut.save(payment);
        }

        return new VnpayIpnResult("00", "Confirm Success");
    }

    @Override
    @Transactional
    public VnpayReturnResult verifyReturn(VnpayCallbackCommand command) {
        VnpayCallbackData callback = verifyCallback(command);
        Payment payment = StringUtils.hasText(callback.transactionRef())
                ? paymentPortOut.findByTransactionRefForUpdate(callback.transactionRef()).orElse(null)
                : null;

        if (callback.validSignature()
                && payment != null
                && PaymentStatus.PENDING.equals(payment.getStatus())) {
            if (callback.isSuccessful()) {
                completeSuccessfulPayment(payment, callback);
                payment = paymentPortOut.findByTransactionRefForUpdate(callback.transactionRef())
                        .orElse(payment);
            } else {
                paymentPolicy.markVnpayFailed(
                        payment,
                        callback.providerTransactionNo(),
                        callback.responseCode(),
                        callback.transactionStatus(),
                        callback.bankCode(),
                        callback.cardType()
                );
                payment = paymentPortOut.save(payment);
            }
        }

        PaymentStatus paymentStatus = payment == null ? null : payment.getStatus();

        return new VnpayReturnResult(
                callback.validSignature(),
                callback.isSuccessful(),
                callback.transactionRef(),
                callback.responseCode(),
                callback.transactionStatus(),
                paymentStatus
        );
    }

    private void validateVnpayAmount(Invoice invoice) {
        if (invoice.getFinalAmount().compareTo(vnpayMinimumAmount) < 0) {
            throw new BadRequestException(
                    "VNPAY requires a minimum payment amount of " + vnpayMinimumAmount.toPlainString() + " VND"
            );
        }
    }

    private void completeSuccessfulPayment(Payment payment, VnpayCallbackData callback) {
        Invoice invoice = invoicePortOut.findById(payment.getInvoiceId())
                .orElseThrow(() -> new NotFoundException("Invoice not found"));
        if (!InvoiceStatus.UNPAID.equals(invoice.getStatus())) {
            throw new ConflictException("Invoice is no longer payable");
        }

        Instant paidAt = callback.paidAt() == null ? Instant.now() : callback.paidAt();
        paymentPolicy.markVnpaySuccessful(
                payment,
                paidAt,
                callback.providerTransactionNo(),
                callback.responseCode(),
                callback.transactionStatus(),
                callback.bankCode(),
                callback.cardType()
        );
        paymentPortOut.save(payment);

        invoicePolicy.markPaid(invoice, paidAt);
        invoicePortOut.save(invoice);
        if (invoice.getSubscriptionId() != null) {
            subscriptionPortIn.markSubscriptionPaymentCompleted(invoice.getSubscriptionId());
        }
        parkingCheckoutCompletionPortIn.completePaidCheckout(invoice.getInvoiceId());
    }

    private VnpayCallbackData verifyCallback(VnpayCallbackCommand command) {
        Map<String, String> parameters = command == null ? null : command.parameters();
        if (parameters == null || parameters.isEmpty()) {
            return new VnpayCallbackData(
                    false,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }
        return vnpayGatewayPortOut.verifyCallback(parameters);
    }

    private void validatePayableInvoice(Invoice invoice) {
        if (!InvoiceStatus.UNPAID.equals(invoice.getStatus())) {
            throw new ConflictException("Only unpaid invoice can be paid");
        }
        if (invoice.getFinalAmount() == null || invoice.getFinalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Invoice amount must be greater than zero");
        }
    }

    private String buildOrderInfo(Invoice invoice) {
        String invoiceReference = StringUtils.hasText(invoice.getInvoiceNo())
                ? invoice.getInvoiceNo().replaceAll("[^A-Za-z0-9 ]", "")
                : invoice.getInvoiceId().toString().replace("-", "");
        return "Thanh toan hoa don " + invoiceReference;
    }

    private String normalizeBankCode(String bankCode) {
        if (!StringUtils.hasText(bankCode)) {
            return null;
        }
        String normalized = bankCode.trim().toUpperCase();
        if (!normalized.matches("[A-Z0-9]{3,20}")) {
            throw new BadRequestException("bankCode is invalid");
        }
        return normalized;
    }
}
