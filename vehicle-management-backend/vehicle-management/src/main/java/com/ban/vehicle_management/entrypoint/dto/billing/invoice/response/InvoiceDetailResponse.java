package com.ban.vehicle_management.entrypoint.dto.billing.invoice.response;

import java.time.Instant;

import com.ban.vehicle_management.entrypoint.dto.billing.payment.response.PaymentResponse;
import com.ban.vehicle_management.shared.enumeration.billing.InvoiceStatus;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record InvoiceDetailResponse(
        UUID invoiceId,
        String invoiceNo,
        UUID customerId,
        UUID parkingSessionId,
        UUID subscriptionId,
        UUID lostCardReportId,
        BigDecimal amount,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        InvoiceStatus status,
        Instant issuedAt,
        Instant paidAt,
        Instant createdAt,
        UUID createdBy,
        Instant updatedAt,
        UUID updatedBy,
        List<PaymentResponse> payments
) {
}
