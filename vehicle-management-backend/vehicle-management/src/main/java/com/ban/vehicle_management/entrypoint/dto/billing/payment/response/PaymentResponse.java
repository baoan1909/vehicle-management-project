package com.ban.vehicle_management.entrypoint.dto.billing.payment.response;

import com.ban.vehicle_management.shared.enumeration.billing.PaymentMethod;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResponse(
        UUID paymentId,
        UUID invoiceId,
        PaymentMethod paymentMethod,
        BigDecimal amount,
        String transactionRef,
        PaymentStatus status,
        String paidAt,
        UUID receivedBy,
        String note
) {
}