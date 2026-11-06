package com.ban.vehicle_management.application.accesscontrol.subscription.model.result;

import com.ban.vehicle_management.domain.accesscontrol.subscription.model.SubscriptionRenewal;
import com.ban.vehicle_management.domain.billing.invoice.model.Invoice;

public record SubscriptionRenewalResult(SubscriptionRenewal renewal, Invoice invoice) {
}
