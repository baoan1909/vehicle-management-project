package com.ban.vehicle_management.domain.billing.voucherredemption.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.billing.VoucherRedemptionStatus;
import com.ban.vehicle_management.shared.enumeration.catalog.VoucherDiscountType;
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
public class VoucherRedemption extends AuditableDomainModel {

    private UUID voucherRedemptionId;
    private UUID voucherId;
    private UUID invoiceId;
    private UUID subscriptionId;
    private UUID customerId;
    private String voucherCodeSnapshot;
    private VoucherDiscountType discountTypeSnapshot;
    private BigDecimal discountValueSnapshot;
    private BigDecimal discountAmount;
    private VoucherRedemptionStatus status;
    private Instant reservedAt;
    private Instant redeemedAt;
    private Instant releasedAt;
    private String releaseReason;
}
