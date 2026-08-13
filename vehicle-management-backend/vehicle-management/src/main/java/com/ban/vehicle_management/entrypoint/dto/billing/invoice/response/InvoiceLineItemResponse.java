package com.ban.vehicle_management.entrypoint.dto.billing.invoice.response;

import java.math.BigDecimal;

public record InvoiceLineItemResponse(
        String code,
        String description,
        BigDecimal amount
) {
}
