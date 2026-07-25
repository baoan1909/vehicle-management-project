package com.ban.vehicle_management.application.billing.payment.model;

import java.math.BigDecimal;
import java.time.Instant;

public record VnpayPaymentRequest(
        String transactionRef,
        BigDecimal amount,
        String orderInfo,
        String clientIp,
        String bankCode,
        String locale,
        Instant createdAt
) {
}
