package com.ban.vehicle_management.entrypoint.dto.accesscontrol.subscription.response;

import com.ban.vehicle_management.entrypoint.dto.billing.invoice.response.InvoiceUserResponse;

public record SubscriptionRenewalResponse(
        SubscriptionRenewalDetailResponse renewal,
        InvoiceUserResponse invoice
) {
}
