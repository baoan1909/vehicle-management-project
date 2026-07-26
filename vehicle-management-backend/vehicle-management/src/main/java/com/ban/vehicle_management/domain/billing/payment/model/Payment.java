package com.ban.vehicle_management.domain.billing.payment.model;

import com.ban.vehicle_management.shared.enumeration.billing.PaymentMethod;
import com.ban.vehicle_management.shared.enumeration.billing.PaymentStatus;
import java.math.BigDecimal;
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
public class Payment {

    private UUID paymentId;
    private UUID invoiceId;
    private PaymentMethod paymentMethod;
    private BigDecimal amount;
    private String transactionRef;
    private PaymentStatus status;
    private Instant paidAt;
    private UUID receivedBy;
    private String note;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant expiresAt;
    private String providerTransactionNo;
    private String providerResponseCode;
    private String providerTransactionStatus;
    private String bankCode;
    private String cardType;
    private String failureReason;
}

