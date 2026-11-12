package com.ban.vehicle_management.domain.iam.account.model;

import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.LostCardReportStatus;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.PhysicalCardFulfillmentStatus;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionRenewalStatus;
import com.ban.vehicle_management.shared.enumeration.accesscontrol.SubscriptionStatus;
import com.ban.vehicle_management.shared.enumeration.parking.CustomerReservationStatus;

public record CustomerDeletionInvoiceContext(
        Invoice invoice,
        boolean pendingPayment,
        SubscriptionStatus subscriptionStatus,
        PhysicalCardFulfillmentStatus physicalCardStatus,
        SubscriptionRenewalStatus renewalStatus,
        CustomerReservationStatus reservationStatus,
        boolean reservationConsumed,
        LostCardReportStatus lostCardReportStatus
) {
}
