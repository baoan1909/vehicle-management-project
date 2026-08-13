package com.ban.vehicle_management.application.billing.invoice.model.result;

import java.math.BigDecimal;

public record InvoiceLineItemResult(
        String code,
        String description,
        BigDecimal amount
) {
}
