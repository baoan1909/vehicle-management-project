package com.ban.vehicle_management.entrypoint.dto.billing.payment.request;

import com.ban.vehicle_management.shared.enumeration.billing.PaymentMethod;
import java.math.BigDecimal;

public record CreatePaymentRequest(
        PaymentMethod paymentMethod,
        BigDecimal amount,
        String transactionRef,
        String note
) {
}