package com.ban.vehicle_management.domain.billing.wallet.model;

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
public class RevenueAllocationReversal {
    private UUID revenueAllocationReversalId;
    private UUID revenueAllocationId;
    private UUID refundPaymentId;
    private BigDecimal refundAmount;
    private BigDecimal reversalRatio;
    private BigDecimal platformFeeAmount;
    private BigDecimal voucherCommissionAmount;
    private BigDecimal partnerPayableAmount;
    private BigDecimal gatewayFeeAmount;
    private BigDecimal discountAmount;
    private UUID financialTransactionId;
    private Instant createdAt;
}
