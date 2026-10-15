package com.ban.vehicle_management.domain.billing.wallet.model;

import com.ban.vehicle_management.shared.enumeration.billing.CommissionBasis;
import com.ban.vehicle_management.shared.enumeration.billing.VoucherFundingSource;
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
public class VoucherFinancialTerm {

    private UUID voucherFinancialTermId;
    private UUID voucherId;
    private VoucherFundingSource fundingSource;
    private UUID fundingOrganizationId;
    private String commissionBeneficiaryType;
    private UUID commissionBeneficiaryId;
    private BigDecimal commissionRate;
    private CommissionBasis commissionBasis;
    private BigDecimal maxCommissionAmount;
    private int settlementDelayDays = 7;
    private Instant validFrom;
    private Instant validTo;
    private String status;
}
