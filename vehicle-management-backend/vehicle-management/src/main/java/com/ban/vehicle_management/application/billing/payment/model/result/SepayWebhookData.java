package com.ban.vehicle_management.application.billing.payment.model.result;

import java.math.BigDecimal;
import java.time.Instant;

public record SepayWebhookData(
        boolean authenticated,
        String transactionRef,
        BigDecimal amount,
        String providerTransactionNo,
        String bankCode,
        String accountNumber,
        String transferType,
        Instant paidAt
) { }
