package com.ban.vehicle_management.domain.billing.wallet.model;

import com.ban.vehicle_management.shared.enumeration.billing.FinancialTransactionStatus;
import com.ban.vehicle_management.shared.enumeration.billing.FinancialTransactionType;
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
public class FinancialTransaction {

    private UUID financialTransactionId;
    private String transactionCode;
    private FinancialTransactionType transactionType;
    private FinancialTransactionStatus status;
    private String idempotencyKey;
    private String referenceType;
    private UUID referenceId;
    private String currency;
    private Instant occurredAt;
    private UUID reversedTransactionId;
    private Instant createdAt;
    private UUID createdBy;
}
