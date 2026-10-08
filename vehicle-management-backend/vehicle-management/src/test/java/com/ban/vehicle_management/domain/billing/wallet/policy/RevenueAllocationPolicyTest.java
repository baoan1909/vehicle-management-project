package com.ban.vehicle_management.domain.billing.wallet.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.billing.wallet.model.VoucherFinancialTerm;
import com.ban.vehicle_management.shared.enumeration.billing.CommissionBasis;
import com.ban.vehicle_management.shared.enumeration.billing.VoucherFundingSource;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RevenueAllocationPolicyTest {

    private final RevenueAllocationPolicy policy = new RevenueAllocationPolicy();

    @Test
    void partnerFundedVoucherExample() {
        // Spec example: gross 200k, discount 20k, paid 180k, fee 9k, commission 900.
        VoucherFinancialTerm term = new VoucherFinancialTerm();
        term.setFundingSource(VoucherFundingSource.PARTNER);
        term.setCommissionBasis(CommissionBasis.PLATFORM_FEE);
        term.setCommissionRate(new BigDecimal("0.10"));
        term.setSettlementDelayDays(7);

        var allocation = policy.allocate(
                UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("200000"), new BigDecimal("20000"), new BigDecimal("180000"),
                new BigDecimal("0.05"), BigDecimal.ZERO, term, 7, Instant.now());

        assertEquals(new BigDecimal("9000.00"), allocation.getPlatformFeeAmount());
        assertEquals(new BigDecimal("900.00"), allocation.getVoucherCommissionAmount());
        assertEquals(new BigDecimal("171000.00"), allocation.getPartnerPayableAmount());
    }

    @Test
    void platformFundedVoucherAddsDiscountBack() {
        VoucherFinancialTerm term = new VoucherFinancialTerm();
        term.setFundingSource(VoucherFundingSource.PLATFORM);
        term.setCommissionBasis(CommissionBasis.PLATFORM_FEE);
        term.setCommissionRate(new BigDecimal("0.10"));
        term.setSettlementDelayDays(7);

        var allocation = policy.allocate(
                UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("200000"), new BigDecimal("20000"), new BigDecimal("180000"),
                new BigDecimal("0.05"), BigDecimal.ZERO, term, 7, Instant.now());

        assertEquals(new BigDecimal("191000.00"), allocation.getPartnerPayableAmount());
    }

    @Test
    void commissionCapApplied() {
        VoucherFinancialTerm term = new VoucherFinancialTerm();
        term.setFundingSource(VoucherFundingSource.PARTNER);
        term.setCommissionBasis(CommissionBasis.PLATFORM_FEE);
        term.setCommissionRate(new BigDecimal("0.50"));
        term.setMaxCommissionAmount(new BigDecimal("1000"));
        term.setSettlementDelayDays(7);

        var allocation = policy.allocate(
                UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("200000"), new BigDecimal("20000"), new BigDecimal("180000"),
                new BigDecimal("0.05"), BigDecimal.ZERO, term, 7, Instant.now());

        assertEquals(new BigDecimal("1000.00"), allocation.getVoucherCommissionAmount());
    }

    @Test
    void changingTermDoesNotRewriteOldAllocation() {
        VoucherFinancialTerm oldTerm = new VoucherFinancialTerm();
        oldTerm.setFundingSource(VoucherFundingSource.PARTNER);
        oldTerm.setCommissionBasis(CommissionBasis.PLATFORM_FEE);
        oldTerm.setCommissionRate(new BigDecimal("0.10"));
        oldTerm.setSettlementDelayDays(7);

        var oldAllocation = policy.allocate(
                UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("200000"), new BigDecimal("20000"), new BigDecimal("180000"),
                new BigDecimal("0.05"), BigDecimal.ZERO, oldTerm, 7, Instant.now());

        // New config must not mutate the snapshot of the old allocation.
        assertEquals("PARTNER", oldAllocation.getFundingSourceSnapshot());
        assertEquals(new BigDecimal("171000.00"), oldAllocation.getPartnerPayableAmount());

        VoucherFinancialTerm newTerm = new VoucherFinancialTerm();
        newTerm.setFundingSource(VoucherFundingSource.PLATFORM);
        newTerm.setCommissionBasis(CommissionBasis.PLATFORM_FEE);
        newTerm.setCommissionRate(new BigDecimal("0.10"));
        newTerm.setSettlementDelayDays(7);
        var newAllocation = policy.allocate(
                UUID.randomUUID(), UUID.randomUUID(), null, UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("200000"), new BigDecimal("20000"), new BigDecimal("180000"),
                new BigDecimal("0.05"), BigDecimal.ZERO, newTerm, 7, Instant.now());
        assertEquals("PLATFORM", newAllocation.getFundingSourceSnapshot());
        assertEquals(new BigDecimal("191000.00"), newAllocation.getPartnerPayableAmount());
        // Old snapshot stays intact.
        assertEquals(new BigDecimal("171000.00"), oldAllocation.getPartnerPayableAmount());
    }
}
