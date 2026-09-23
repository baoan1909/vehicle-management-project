package com.ban.vehicle_management.infrastructure.persistence.database.entity.billing;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.billing.VoucherRedemptionStatus;
import com.ban.vehicle_management.shared.enumeration.catalog.VoucherDiscountType;
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
@Table(name = "voucher_redemptions", schema = "billing")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VoucherRedemptionEntity extends AuditableEntity {
    @Id
    @Column(name = "voucher_redemption_id", nullable = false)
    private UUID voucherRedemptionId;
    @Column(name = "voucher_id", nullable = false)
    private UUID voucherId;
    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;
    @Column(name = "subscription_id", nullable = false)
    private UUID subscriptionId;
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;
    @Column(name = "voucher_code_snapshot", nullable = false, length = 50)
    private String voucherCodeSnapshot;
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type_snapshot", nullable = false)
    private VoucherDiscountType discountTypeSnapshot;
    @Column(name = "discount_value_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValueSnapshot;
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VoucherRedemptionStatus status;
    @Column(name = "reserved_at", nullable = false)
    private Instant reservedAt;
    @Column(name = "redeemed_at")
    private Instant redeemedAt;
    @Column(name = "released_at")
    private Instant releasedAt;
    @Column(name = "release_reason", length = 500)
    private String releaseReason;
}
