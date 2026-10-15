package com.ban.vehicle_management.entrypoint.dto.billing.wallet.response;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransactionResponse {

    private UUID financialTransactionId;
    private String transactionCode;
    private String transactionType;
    private String status;
    private String currency;
    private Instant occurredAt;
    private UUID referenceId;
}
