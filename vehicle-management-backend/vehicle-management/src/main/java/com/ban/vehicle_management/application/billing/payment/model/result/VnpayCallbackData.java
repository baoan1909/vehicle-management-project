package com.ban.vehicle_management.application.billing.payment.model.result;

import java.math.BigDecimal;
import java.time.Instant;

public record VnpayCallbackData(
        boolean validSignature,
        String terminalCode,
        String transactionRef,
        BigDecimal amount,
        String responseCode,
        String transactionStatus,
        String providerTransactionNo,
        String bankCode,
        String cardType,
        Instant paidAt
) {
    public boolean isSuccessful() {
        return validSignature
                && "00".equals(responseCode)
                && "00".equals(transactionStatus);
    }
}
