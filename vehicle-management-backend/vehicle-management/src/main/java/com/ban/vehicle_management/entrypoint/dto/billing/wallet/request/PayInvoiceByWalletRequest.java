package com.ban.vehicle_management.entrypoint.dto.billing.wallet.request;

import java.util.UUID;

public record PayInvoiceByWalletRequest(
        UUID invoiceId,
        String idempotencyKey
) {
}
