package com.ban.vehicle_management.infrastructure.persistence.database.entity.billing;

import com.ban.vehicle_management.shared.enumeration.billing.RevenueAllocationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "revenue_allocations", schema = "billing")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RevenueAllocationEntity {

    @Id
    @Column(name = "revenue_allocation_id", nullable = false)
    private UUID revenueAllocationId;

    @Column(name = "payment_id", nullable = false, unique = true)
    private UUID paymentId;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "parking_lot_id")
    private UUID parkingLotId;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "voucher_id")
    private UUID voucherId;

    @Column(name = "gross_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal grossAmount;

    @Column(name = "discount_amount", precision = 19, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "customer_paid_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal customerPaidAmount;

    @Column(name = "sponsored_discount_amount", precision = 19, scale = 2)
    private BigDecimal sponsoredDiscountAmount;

    @Column(name = "platform_fee_amount", precision = 19, scale = 2)
    private BigDecimal platformFeeAmount;

    @Column(name = "voucher_commission_amount", precision = 19, scale = 2)
    private BigDecimal voucherCommissionAmount;

    @Column(name = "partner_payable_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal partnerPayableAmount;

    @Column(name = "gateway_fee_amount", precision = 19, scale = 2)
    private BigDecimal gatewayFeeAmount;

    @Column(name = "funding_source_snapshot")
    private String fundingSourceSnapshot;

    @Column(name = "commission_rate_snapshot", precision = 9, scale = 6)
    private BigDecimal commissionRateSnapshot;

    @Column(name = "commission_basis_snapshot")
    private String commissionBasisSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RevenueAllocationStatus status;

    @Column(name = "available_at")
    private Instant availableAt;

    @Column(name = "settled_at")
    private Instant settledAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
