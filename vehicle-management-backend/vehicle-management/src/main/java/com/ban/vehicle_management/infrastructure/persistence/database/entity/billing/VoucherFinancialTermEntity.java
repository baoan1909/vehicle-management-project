package com.ban.vehicle_management.infrastructure.persistence.database.entity.billing;

import com.ban.vehicle_management.shared.enumeration.billing.CommissionBasis;
import com.ban.vehicle_management.shared.enumeration.billing.VoucherFundingSource;
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
@Table(name = "voucher_financial_terms", schema = "catalog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VoucherFinancialTermEntity {

    @Id
    @Column(name = "voucher_financial_term_id", nullable = false)
    private UUID voucherFinancialTermId;

    @Column(name = "voucher_id", nullable = false)
    private UUID voucherId;

    @Enumerated(EnumType.STRING)
    @Column(name = "funding_source", nullable = false)
    private VoucherFundingSource fundingSource;

    @Column(name = "funding_organization_id")
    private UUID fundingOrganizationId;

    @Column(name = "commission_beneficiary_type")
    private String commissionBeneficiaryType;

    @Column(name = "commission_beneficiary_id")
    private UUID commissionBeneficiaryId;

    @Column(name = "commission_rate", precision = 9, scale = 6)
    private BigDecimal commissionRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "commission_basis", nullable = false)
    private CommissionBasis commissionBasis;

    @Column(name = "max_commission_amount", precision = 19, scale = 2)
    private BigDecimal maxCommissionAmount;

    @Column(name = "settlement_delay_days", nullable = false)
    private int settlementDelayDays = 7;

    @Column(name = "valid_from")
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    @Column(name = "status", nullable = false)
    private String status;
}
