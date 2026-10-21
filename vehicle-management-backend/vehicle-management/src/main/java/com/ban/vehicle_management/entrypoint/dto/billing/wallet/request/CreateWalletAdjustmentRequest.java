package com.ban.vehicle_management.entrypoint.dto.billing.wallet.request;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateWalletAdjustmentRequest(
        UUID walletId,
        BigDecimal amount,
        String direction,
        String reason) {
}
