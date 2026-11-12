package com.ban.vehicle_management.domain.iam.account.policy;

import com.ban.vehicle_management.domain.accesscontrol.subscription.model.Subscription;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionAutomaticAction;
import com.ban.vehicle_management.domain.iam.account.model.AccountDeletionBlocker;
import com.ban.vehicle_management.domain.iam.account.model.CustomerDeletionInvoiceContext;
import com.ban.vehicle_management.domain.iam.account.model.CustomerDeletionResourceDecision;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportStatus;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.PhysicalCardFulfillmentStatus;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionRenewalStatus;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import com.ban.vehicle_management.shared.enumeration.billing.InvoicePurpose;
import com.ban.vehicle_management.shared.enumeration.parking.CustomerReservationStatus;
import java.time.Instant;

public class CustomerAccountDeletionPolicy {

    public AccountDeletionAutomaticAction classifyActiveSubscription(
            Subscription subscription, Instant scheduledAt) {
        if (subscription == null || subscription.getStatus() != SubscriptionStatus.ACTIVE) return null;
        return action("TERMINATE_ACTIVE_SUBSCRIPTION",
                "Vé đăng ký đang hoạt động sẽ bị chấm dứt sau thời gian chờ và không được hoàn tiền.",
                "SUBSCRIPTION", subscription.getSubscriptionId(), "TERMINATE_SUBSCRIPTION",
                scheduledAt, "/customer/subscriptions?subscriptionId=" + subscription.getSubscriptionId());
    }

    public CustomerDeletionResourceDecision classifyInvoice(
            CustomerDeletionInvoiceContext context, Instant scheduledAt) {
        Invoice invoice = context.invoice();
        if (context.pendingPayment()) {
            return CustomerDeletionResourceDecision.blocker(blocker(
                    "PENDING_PAYMENT", "Cần chờ giao dịch thanh toán đang xử lý hoàn tất hoặc hết hạn.",
                    "INVOICE", invoice, "REVIEW_PAYMENT"));
        }
        return switch (invoice.getPurpose()) {
            case SUBSCRIPTION_PURCHASE -> classifySubscriptionPurchase(context, scheduledAt);
            case SUBSCRIPTION_RENEWAL -> context.renewalStatus() == SubscriptionRenewalStatus.PENDING_PAYMENT
                    ? automatic(invoice, "CANCEL_UNPAID_SUBSCRIPTION_RENEWAL",
                    "Yêu cầu gia hạn chưa thanh toán sẽ được tự động hủy.", scheduledAt)
                    : debt(invoice, "UNRESOLVED_CUSTOMER_DEBT");
            case SUBSCRIPTION_CARD_DEPOSIT -> context.physicalCardStatus() != PhysicalCardFulfillmentStatus.ASSIGNED
                    ? automatic(invoice, "CANCEL_UNPAID_CARD_DEPOSIT",
                    "Yêu cầu thẻ vật lý chưa được cấp sẽ được tự động hủy.", scheduledAt)
                    : debt(invoice, "UNRESOLVED_CUSTOMER_DEBT");
            case RESERVATION_PREPAYMENT -> context.reservationStatus() == CustomerReservationStatus.PENDING_PAYMENT
                    && !context.reservationConsumed()
                    ? automatic(invoice, "CANCEL_UNPAID_RESERVATION",
                    "Đặt chỗ chưa thanh toán và chưa sử dụng sẽ được tự động hủy.", scheduledAt)
                    : debt(invoice, "UNRESOLVED_CUSTOMER_DEBT");
            case LOST_CARD -> context.lostCardReportStatus() == LostCardReportStatus.OPEN
                    ? automatic(invoice, "CANCEL_UNFULFILLED_LOST_CARD_REQUEST",
                    "Yêu cầu báo mất thẻ chưa được xử lý sẽ được tự động hủy.", scheduledAt)
                    : debt(invoice, "UNPAID_LOST_CARD_OBLIGATION");
            case PARKING_SESSION -> debt(invoice, "UNPAID_CONSUMED_PARKING_INVOICE");
            case MANUAL -> debt(invoice, "UNRESOLVED_CUSTOMER_DEBT");
        };
    }

    private CustomerDeletionResourceDecision classifySubscriptionPurchase(
            CustomerDeletionInvoiceContext context, Instant scheduledAt) {
        SubscriptionStatus status = context.subscriptionStatus();
        if (status == SubscriptionStatus.PENDING || status == SubscriptionStatus.PENDING_PAYMENT) {
            return automatic(context.invoice(), "CANCEL_UNPAID_SUBSCRIPTION_PURCHASE",
                    "Yêu cầu đăng ký vé chưa thanh toán sẽ được tự động hủy.", scheduledAt);
        }
        return debt(context.invoice(), "UNRESOLVED_CUSTOMER_DEBT");
    }

    private CustomerDeletionResourceDecision automatic(
            Invoice invoice, String code, String message, Instant scheduledAt) {
        return CustomerDeletionResourceDecision.automatic(action(code, message, "INVOICE",
                invoice.getInvoiceId(), code, scheduledAt,
                "/customer/invoices?invoiceId=" + invoice.getInvoiceId()));
    }

    private CustomerDeletionResourceDecision debt(Invoice invoice, String code) {
        String message = switch (code) {
            case "UNPAID_CONSUMED_PARKING_INVOICE" -> "Cần thanh toán phí gửi xe của dịch vụ đã sử dụng.";
            case "UNPAID_LOST_CARD_OBLIGATION" -> "Cần thanh toán phí mất thẻ đã phát sinh.";
            default -> "Cần xử lý khoản công nợ đã được xác nhận.";
        };
        return CustomerDeletionResourceDecision.blocker(blocker(code, message, "INVOICE", invoice, "PAY_INVOICE"));
    }

    private AccountDeletionBlocker blocker(
            String code, String message, String resourceType, Invoice invoice, String action) {
        return new AccountDeletionBlocker(code, message, resourceType, invoice.getInvoiceId(), null,
                invoice.getParkingLotId(), action,
                "/customer/wallet/topup?invoiceId=" + invoice.getInvoiceId());
    }

    private AccountDeletionAutomaticAction action(
            String code, String message, String resourceType, java.util.UUID resourceId,
            String action, Instant scheduledAt, String remediationUrl) {
        return new AccountDeletionAutomaticAction(code, message, resourceType, resourceId,
                action, scheduledAt, null, remediationUrl);
    }
}
