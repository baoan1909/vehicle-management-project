package com.ban.vehicle_management.entrypoint.dto.billing.payment.request;

import com.ban.vehicle_management.shared.enumeration.billing.PaymentMethod;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;
import java.time.Instant;
import java.util.UUID;

public record PaymentFilterRequest(
        UUID invoiceId,
        PaymentMethod paymentMethod,
        PaymentStatus status,
        UUID receivedBy,
        Instant fromDate,
        Instant toDate,
        String keyword
) {
}