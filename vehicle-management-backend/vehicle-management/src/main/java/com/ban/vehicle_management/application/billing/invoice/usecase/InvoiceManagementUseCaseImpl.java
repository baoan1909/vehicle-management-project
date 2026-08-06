package com.ban.vehicle_management.application.billing.invoice.usecase;

import com.ban.vehicle_management.application.accesscontrol.lostcardreport.port.out.LostCardReportPortOut;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionPortOut;
import com.ban.vehicle_management.application.billing.invoice.authorization.InvoiceAccessGuard;
import com.ban.vehicle_management.application.billing.invoice.model.result.InvoiceLineItemResult;
import com.ban.vehicle_management.application.billing.invoice.model.result.InvoiceManagementDetailResult;
import com.ban.vehicle_management.application.billing.invoice.model.result.InvoiceManagementItemResult;
import com.ban.vehicle_management.application.billing.invoice.model.result.InvoiceManagementPageResult;
import com.ban.vehicle_management.application.billing.invoice.model.result.InvoiceManagementSummaryResult;
import com.ban.vehicle_management.application.billing.invoice.port.in.InvoiceManagementPortIn;
import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.application.billing.payment.port.out.PaymentPortOut;
import com.ban.vehicle_management.application.parking.parkingsession.port.out.ParkingSessionPortOut;
import com.ban.vehicle_management.application.people.customer.port.out.CustomerPortOut;
import com.ban.vehicle_management.application.people.customervehicle.port.out.CustomerVehiclePortOut;
import com.ban.vehicle_management.application.people.userprofile.port.out.UserProfilePortOut;
import com.ban.vehicle_management.domain.accesscontrol.lostcardreport.model.LostCardReport;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.billing.payment.model.Payment;
import com.ban.vehicle_management.domain.parking.parkingsession.model.ParkingSession;
import com.ban.vehicle_management.domain.people.customer.model.Customer;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceSource;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentMethod;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceManagementUseCaseImpl implements InvoiceManagementPortIn {

    private static final int MAX_PAGE_SIZE = 5000;

    private final InvoicePortOut invoicePortOut;
    private final PaymentPortOut paymentPortOut;
    private final InvoiceAccessGuard invoiceAccessGuard;
    private final CustomerPortOut customerPortOut;
    private final UserProfilePortOut userProfilePortOut;
    private final CustomerVehiclePortOut customerVehiclePortOut;
    private final ParkingSessionPortOut parkingSessionPortOut;
    private final SubscriptionPortOut subscriptionPortOut;
    private final LostCardReportPortOut lostCardReportPortOut;

    public InvoiceManagementUseCaseImpl(
            InvoicePortOut invoicePortOut,
            PaymentPortOut paymentPortOut,
            InvoiceAccessGuard invoiceAccessGuard,
            CustomerPortOut customerPortOut,
            UserProfilePortOut userProfilePortOut,
            CustomerVehiclePortOut customerVehiclePortOut,
            ParkingSessionPortOut parkingSessionPortOut,
            SubscriptionPortOut subscriptionPortOut,
            LostCardReportPortOut lostCardReportPortOut
    ) {
        this.invoicePortOut = invoicePortOut;
        this.paymentPortOut = paymentPortOut;
        this.invoiceAccessGuard = invoiceAccessGuard;
        this.customerPortOut = customerPortOut;
        this.userProfilePortOut = userProfilePortOut;
        this.customerVehiclePortOut = customerVehiclePortOut;
        this.parkingSessionPortOut = parkingSessionPortOut;
        this.subscriptionPortOut = subscriptionPortOut;
        this.lostCardReportPortOut = lostCardReportPortOut;
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceManagementPageResult getInvoices(
            InvoiceStatus status,
            PaymentMethod paymentMethod,
            Instant fromDate,
            Instant toDate,
            String keyword,
            int page,
            int size
    ) {
        invoiceAccessGuard.ensureCanReadAll();

        int resolvedPage = Math.max(page, 0);
        int resolvedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String normalizedKeyword = normalizeKeyword(keyword);

        List<InvoiceManagementItemResult> filteredItems = invoicePortOut.findAll(
                        null,
                        null,
                        null,
                        null,
                        status,
                        fromDate,
                        toDate,
                        null
                ).stream()
                .map(this::toManagementItem)
                .filter(item -> paymentMethod == null || paymentMethod.equals(item.paymentMethod()))
                .filter(item -> matchesKeyword(item, normalizedKeyword))
                .sorted(Comparator.comparing(
                        InvoiceManagementItemResult::createdAt,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();

        int fromIndex = Math.min(resolvedPage * resolvedSize, filteredItems.size());
        int toIndex = Math.min(fromIndex + resolvedSize, filteredItems.size());
        int totalPages = filteredItems.isEmpty()
                ? 1
                : (int) Math.ceil((double) filteredItems.size() / resolvedSize);

        return new InvoiceManagementPageResult(
                filteredItems.subList(fromIndex, toIndex),
                resolvedPage,
                resolvedSize,
                filteredItems.size(),
                totalPages
        );
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceManagementSummaryResult getSummary() {
        invoiceAccessGuard.ensureCanReadAll();
        List<Invoice> invoices = invoicePortOut.findAll(
                null, null, null, null, null, null, null, null
        );

        return new InvoiceManagementSummaryResult(
                invoices.size(),
                countByStatus(invoices, InvoiceStatus.UNPAID),
                countByStatus(invoices, InvoiceStatus.PAID),
                countByStatus(invoices, InvoiceStatus.CANCELLED),
                countByStatus(invoices, InvoiceStatus.REFUNDED)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceManagementDetailResult getInvoiceDetail(UUID invoiceId) {
        invoiceAccessGuard.ensureCanReadAll();
        Invoice invoice = invoicePortOut.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found"));

        return new InvoiceManagementDetailResult(
                toManagementItem(invoice),
                resolveLineItems(invoice),
                paymentPortOut.findByInvoiceId(invoiceId)
        );
    }

    private InvoiceManagementItemResult toManagementItem(Invoice invoice) {
        InvoiceContext context = resolveContext(invoice);
        Payment payment = resolveDisplayPayment(invoice.getInvoiceId());

        return new InvoiceManagementItemResult(
                invoice.getInvoiceId(),
                invoice.getInvoiceNo(),
                invoice.getCustomerId(),
                context.customerName(),
                context.licensePlate(),
                context.source(),
                context.sourceId(),
                invoice.getAmount(),
                invoice.getDiscountAmount(),
                invoice.getFinalAmount(),
                invoice.getStatus(),
                payment == null ? null : payment.getPaymentMethod(),
                payment == null ? null : payment.getStatus(),
                payment == null ? null : payment.getTransactionRef(),
                invoice.getIssuedAt(),
                invoice.getPaidAt(),
                invoice.getCreatedAt(),
                invoice.getUpdatedAt()
        );
    }

    private InvoiceContext resolveContext(Invoice invoice) {
        if (invoice.getLostCardReportId() != null) {
            LostCardReport report = lostCardReportPortOut.findById(invoice.getLostCardReportId()).orElse(null);
            String customerName = resolveCustomerName(invoice.getCustomerId());
            String licensePlate = report == null ? null : resolveLostCardLicensePlate(report);
            if (isBlank(customerName) && report != null) customerName = report.getReporterName();
            return new InvoiceContext(
                    defaultText(customerName, "Khách vãng lai"),
                    licensePlate,
                    InvoiceSource.LOST_CARD,
                    invoice.getLostCardReportId()
            );
        }

        if (invoice.getSubscriptionId() != null) {
            Subscription subscription = subscriptionPortOut.findById(invoice.getSubscriptionId()).orElse(null);
            String licensePlate = subscription == null
                    ? null
                    : customerVehiclePortOut.findById(subscription.getCustomerVehicleId())
                            .map(vehicle -> vehicle.getLicensePlate())
                            .orElse(null);
            return new InvoiceContext(
                    defaultText(resolveCustomerName(invoice.getCustomerId()), "Khách hàng"),
                    licensePlate,
                    InvoiceSource.SUBSCRIPTION,
                    invoice.getSubscriptionId()
            );
        }

        if (invoice.getParkingSessionId() != null) {
            ParkingSession session = parkingSessionPortOut.findById(invoice.getParkingSessionId()).orElse(null);
            String licensePlate = session == null
                    ? null
                    : defaultText(session.getLicensePlateOut(), session.getLicensePlateIn());
            return new InvoiceContext(
                    defaultText(resolveCustomerName(invoice.getCustomerId()), "Khách vãng lai"),
                    licensePlate,
                    InvoiceSource.PARKING_SESSION,
                    invoice.getParkingSessionId()
            );
        }

        return new InvoiceContext(
                defaultText(resolveCustomerName(invoice.getCustomerId()), "Khách vãng lai"),
                null,
                InvoiceSource.MANUAL,
                null
        );
    }

    private List<InvoiceLineItemResult> resolveLineItems(Invoice invoice) {
        List<InvoiceLineItemResult> items = new ArrayList<>();

        if (invoice.getLostCardReportId() != null) {
            lostCardReportPortOut.findById(invoice.getLostCardReportId()).ifPresent(report -> {
                addPositiveItem(items, "PARKING_FEE", "Phí gửi xe", report.getTicketPrice());
                addPositiveItem(items, "LOST_CARD_FEE", "Phí mất thẻ", report.getLostCardFee());
            });
        } else if (invoice.getSubscriptionId() != null) {
            addPositiveItem(items, "SUBSCRIPTION_FEE", "Phí đăng ký vé", invoice.getAmount());
        } else if (invoice.getParkingSessionId() != null) {
            addPositiveItem(items, "PARKING_FEE", "Phí gửi xe", invoice.getAmount());
        } else {
            addPositiveItem(items, "OTHER", "Khoản thu khác", invoice.getAmount());
        }

        if (items.isEmpty()) {
            items.add(new InvoiceLineItemResult("TOTAL", "Giá trị hóa đơn", zeroIfNull(invoice.getAmount())));
        }
        return List.copyOf(items);
    }

    private String resolveLostCardLicensePlate(LostCardReport report) {
        if (report.getParkingSessionId() != null) {
            return parkingSessionPortOut.findById(report.getParkingSessionId())
                    .map(session -> defaultText(session.getLicensePlateOut(), session.getLicensePlateIn()))
                    .orElse(null);
        }
        if (report.getSubscriptionId() != null) {
            return subscriptionPortOut.findById(report.getSubscriptionId())
                    .flatMap(subscription -> customerVehiclePortOut.findById(subscription.getCustomerVehicleId()))
                    .map(vehicle -> vehicle.getLicensePlate())
                    .orElse(null);
        }
        return null;
    }

    private String resolveCustomerName(UUID customerId) {
        if (customerId == null) return null;

        Customer customer = customerPortOut.findById(customerId).orElse(null);
        if (customer == null) return null;
        if (customer.getUserProfile() != null && !isBlank(customer.getUserProfile().getFullName())) {
            return customer.getUserProfile().getFullName();
        }
        if (customer.getUserProfileId() != null) {
            String profileName = userProfilePortOut.findById(customer.getUserProfileId())
                    .map(profile -> profile.getFullName())
                    .orElse(null);
            if (!isBlank(profileName)) return profileName;
        }
        return customer.getCustomerCode();
    }

    private Payment resolveDisplayPayment(UUID invoiceId) {
        List<Payment> payments = paymentPortOut.findByInvoiceId(invoiceId);
        return latestPayment(payments, PaymentStatus.SUCCESS)
                .orElseGet(() -> latestPayment(payments, PaymentStatus.PENDING)
                        .orElseGet(() -> payments.stream().max(paymentComparator()).orElse(null)));
    }

    private java.util.Optional<Payment> latestPayment(List<Payment> payments, PaymentStatus status) {
        return payments.stream()
                .filter(payment -> status.equals(payment.getStatus()))
                .max(paymentComparator());
    }

    private Comparator<Payment> paymentComparator() {
        return Comparator.comparing(
                this::paymentTimestamp,
                Comparator.nullsFirst(Comparator.naturalOrder())
        );
    }

    private Instant paymentTimestamp(Payment payment) {
        if (payment.getPaidAt() != null) return payment.getPaidAt();
        if (payment.getUpdatedAt() != null) return payment.getUpdatedAt();
        return payment.getCreatedAt();
    }

    private boolean matchesKeyword(InvoiceManagementItemResult item, String keyword) {
        if (keyword == null) return true;
        return contains(item.invoiceNo(), keyword)
                || contains(item.customerName(), keyword)
                || contains(item.licensePlate(), keyword);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String normalizeKeyword(String keyword) {
        return isBlank(keyword) ? null : keyword.trim().toLowerCase(Locale.ROOT);
    }

    private long countByStatus(List<Invoice> invoices, InvoiceStatus status) {
        return invoices.stream().filter(invoice -> status.equals(invoice.getStatus())).count();
    }

    private void addPositiveItem(
            List<InvoiceLineItemResult> items,
            String code,
            String description,
            BigDecimal amount
    ) {
        if (amount != null && amount.signum() > 0) {
            items.add(new InvoiceLineItemResult(code, description, amount));
        }
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String defaultText(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record InvoiceContext(
            String customerName,
            String licensePlate,
            InvoiceSource source,
            UUID sourceId
    ) {
    }
}
