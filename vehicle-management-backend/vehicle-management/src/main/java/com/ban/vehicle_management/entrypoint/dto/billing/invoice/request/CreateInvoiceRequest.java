package com.ban.vehicle_management.entrypoint.dto.billing.invoice.request;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateInvoiceRequest (
        UUID customerId,
        UUID parkingSessionId,
        UUID subscriptionId,
        UUID lostCardReportId,
        BigDecimal amount,
        BigDecimal discountAmount
){
}
