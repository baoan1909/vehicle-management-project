package com.ban.vehicle_management.infrastructure.persistence.database.entity.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "revenue_allocation_reversals", schema = "billing")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RevenueAllocationReversalEntity {
    @Id
    @Column(name = "revenue_allocation_reversal_id", nullable = false)
    private UUID revenueAllocationReversalId;
    @Column(name = "revenue_allocation_id", nullable = false)
    private UUID revenueAllocationId;
    @Column(name = "refund_payment_id", nullable = false, unique = true)
    private UUID refundPaymentId;
    @Column(name = "refund_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal refundAmount;
    @Column(name = "reversal_ratio", nullable = false, precision = 19, scale = 10)
    private BigDecimal reversalRatio;
    @Column(name = "platform_fee_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal platformFeeAmount;
    @Column(name = "voucher_commission_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal voucherCommissionAmount;
    @Column(name = "partner_payable_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal partnerPayableAmount;
    @Column(name = "gateway_fee_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal gatewayFeeAmount;
    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount;
    @Column(name = "financial_transaction_id")
    private UUID financialTransactionId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
