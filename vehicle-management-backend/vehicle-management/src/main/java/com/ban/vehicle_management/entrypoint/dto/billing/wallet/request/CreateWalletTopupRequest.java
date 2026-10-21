package com.ban.vehicle_management.entrypoint.dto.billing.wallet.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateWalletTopupRequest(
        @NotNull BigDecimal amount,
        @Size(max = 100) String idempotencyKey
) {
}
