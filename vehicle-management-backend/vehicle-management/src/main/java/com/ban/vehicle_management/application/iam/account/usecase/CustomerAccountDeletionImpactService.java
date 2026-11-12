package com.ban.vehicle_management.application.iam.account.usecase;

import com.ban.vehicle_management.application.accesscontrol.lostcardreport.port.out.LostCardReportPortOut;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionPortOut;
import com.ban.vehicle_management.application.accesscontrol.subscription.port.out.SubscriptionRenewalPortOut;
import com.ban.vehicle_management.application.billing.invoice.port.out.InvoicePortOut;
import com.ban.vehicle_management.application.billing.payment.port.out.PaymentPortOut;
import com.ban.vehicle_management.application.iam.account.port.out.AccountDeletionPortOut;
import com.ban.vehicle_management.application.parking.customerreservation.port.out.CustomerReservationPortOut;
import com.ban.vehicle_management.domain.accesscontrol.lostcardreport.model.LostCardReport;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.accesscontrol.subscription.model.SubscriptionRenewal;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionAutomaticAction;
import com.ban.vehicle_management.domain.iam.account.model.CustomerDeletionInvoiceContext;
import com.ban.vehicle_management.domain.iam.account.model.CustomerDeletionResourceDecision;
import com.ban.vehicle_management.domain.iam.account.policy.CustomerAccountDeletionPolicy;
import com.ban.vehicle_management.domain.parking.customerreservation.model.CustomerReservation;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportStatus;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CustomerAccountDeletionImpactService {
    private final AccountDeletionPortOut accountDeletionPort;
    private final SubscriptionPortOut subscriptionPort;
    private final SubscriptionRenewalPortOut renewalPort;
    private final InvoicePortOut invoicePort;
    private final PaymentPortOut paymentPort;
    private final CustomerReservationPortOut reservationPort;
    private final LostCardReportPortOut lostCardReportPort;
    private final CustomerAccountDeletionPolicy policy = new CustomerAccountDeletionPolicy();

    public CustomerAccountDeletionImpactService(
            AccountDeletionPortOut accountDeletionPort,
            SubscriptionPortOut subscriptionPort,
            SubscriptionRenewalPortOut renewalPort,
            InvoicePortOut invoicePort,
            PaymentPortOut paymentPort,
            CustomerReservationPortOut reservationPort,
            LostCardReportPortOut lostCardReportPort) {
        this.accountDeletionPort = accountDeletionPort;
        this.subscriptionPort = subscriptionPort;
        this.renewalPort = renewalPort;
        this.invoicePort = invoicePort;
        this.paymentPort = paymentPort;
        this.reservationPort = reservationPort;
        this.lostCardReportPort = lostCardReportPort;
    }

    public CustomerDeletionResourceDecision analyze(UUID accountId, Instant scheduledAt) {
        UUID customerId = accountDeletionPort.findDeletionCustomerId(accountId).orElse(null);
        if (customerId == null) return new CustomerDeletionResourceDecision(List.of(), List.of());

        List<Subscription> subscriptions = subscriptionPort.findAll(
                customerId, null, null, null, null, null, null, null);
        Map<UUID, Subscription> subscriptionsById = new HashMap<>();
        subscriptions.forEach(subscription -> subscriptionsById.put(subscription.getSubscriptionId(), subscription));

        List<CustomerReservation> reservations = reservationPort.findByCustomerId(customerId);
        Map<UUID, CustomerReservation> reservationsByInvoiceId = new HashMap<>();
        reservations.forEach(reservation -> reservationsByInvoiceId.put(reservation.getInvoiceId(), reservation));

        List<LostCardReport> reports = lostCardReportPort
                .findForAccountDeletionByCustomerId(customerId);
        Map<UUID, LostCardReport> reportsById = new HashMap<>();
        reports.forEach(report -> reportsById.put(report.getLostCardReportId(), report));

        List<AccountDeletionAutomaticAction> actions = new java.util.ArrayList<>();
        subscriptions.stream()
                .map(subscription -> policy.classifyActiveSubscription(subscription, scheduledAt))
                .filter(java.util.Objects::nonNull)
                .forEach(actions::add);

        List<com.ban.vehicle_management.domain.iam.account.model.AccountDeletionBlocker> blockers =
                new java.util.ArrayList<>();
        List<Invoice> invoices = invoicePort.findAll(
                customerId, null, null, null, InvoiceStatus.UNPAID, null, null, null);
        for (Invoice invoice : invoices) {
            Subscription subscription = invoice.getSubscriptionId() == null
                    ? null : subscriptionsById.get(invoice.getSubscriptionId());
            SubscriptionRenewal renewal = renewalPort.findByInvoiceId(invoice.getInvoiceId()).orElse(null);
            CustomerReservation reservation = reservationsByInvoiceId.get(invoice.getInvoiceId());
            LostCardReport report = invoice.getLostCardReportId() == null
                    ? null : reportsById.get(invoice.getLostCardReportId());
            CustomerDeletionInvoiceContext context = new CustomerDeletionInvoiceContext(
                    invoice,
                    paymentPort.existsByInvoiceIdAndStatus(invoice.getInvoiceId(), PaymentStatus.PENDING),
                    subscription == null ? null : subscription.getStatus(),
                    subscription == null ? null : subscription.getPhysicalCardStatus(),
                    renewal == null ? null : renewal.getStatus(),
                    reservation == null ? null : reservation.getStatus(),
                    reservation != null && (reservation.getParkingSessionId() != null
                            || reservation.getCheckedInAt() != null),
                    report == null ? null : report.getStatus());
            CustomerDeletionResourceDecision decision = policy.classifyInvoice(context, scheduledAt);
            blockers.addAll(decision.blockers());
            actions.addAll(decision.automaticActions());
        }
        return new CustomerDeletionResourceDecision(blockers, actions);
    }
}
