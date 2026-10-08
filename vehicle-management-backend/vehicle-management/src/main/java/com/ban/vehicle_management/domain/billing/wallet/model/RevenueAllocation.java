package com.ban.vehicle_management.domain.billing.wallet.model;

import com.ban.vehicle_management.shared.enumeration.billing.RevenueAllocationStatus;
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
public class RevenueAllocation {

    private UUID revenueAllocationId;
    private UUID paymentId;
    private UUID invoiceId;
    private UUID parkingLotId;
    private UUID organizationId;
    private UUID voucherId;
    private BigDecimal grossAmount;
    private BigDecimal discountAmount;
    private BigDecimal customerPaidAmount;
    private BigDecimal sponsoredDiscountAmount;
    private BigDecimal platformFeeAmount;
    private BigDecimal voucherCommissionAmount;
    private BigDecimal partnerPayableAmount;
    private BigDecimal gatewayFeeAmount;
    private String fundingSourceSnapshot;
    private BigDecimal commissionRateSnapshot;
    private String commissionBasisSnapshot;
    private RevenueAllocationStatus status;
    private Instant availableAt;
    private Instant settledAt;
    private Instant createdAt;
}
