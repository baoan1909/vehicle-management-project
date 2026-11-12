package com.ban.vehicle_management.domain.billing.wallet.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.billing.wallet.model.RevenueAllocation;
import com.ban.vehicle_management.domain.billing.wallet.model.RevenueAllocationReversal;
import com.ban.vehicle_management.domain.billing.wallet.model.RevenueAllocationReversalTotals;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RevenueAllocationReversalPolicyTest {

    private final RevenueAllocationReversalPolicy policy = new RevenueAllocationReversalPolicy();

    @Test
    void finalPartialRefundAbsorbsRoundingRemainderWithoutExceedingOriginal() {
        RevenueAllocation allocation = allocation();
        RevenueAllocationReversal first = policy.create(
                allocation, UUID.randomUUID(), new BigDecimal("33333"),
                RevenueAllocationReversalTotals.zero());
        RevenueAllocationReversalTotals prior = new RevenueAllocationReversalTotals(
                first.getRefundAmount(), first.getPlatformFeeAmount(),
                first.getVoucherCommissionAmount(), first.getPartnerPayableAmount(),
                first.getGatewayFeeAmount(), first.getDiscountAmount());

        RevenueAllocationReversal last = policy.create(
                allocation, UUID.randomUUID(), new BigDecimal("66667"), prior);

        assertEquals(new BigDecimal("5000.00"),
                first.getPlatformFeeAmount().add(last.getPlatformFeeAmount()));
        assertEquals(new BigDecimal("95000.00"),
                first.getPartnerPayableAmount().add(last.getPartnerPayableAmount()));
        assertEquals(new BigDecimal("1.0000000000"),
                first.getReversalRatio().add(last.getReversalRatio()));
    }

    @Test
    void rejectsCumulativeRefundAboveOriginalPayment() {
        RevenueAllocationReversalTotals prior = new RevenueAllocationReversalTotals(
                new BigDecimal("90000"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        assertThrows(BadRequestException.class, () -> policy.create(
                allocation(), UUID.randomUUID(), new BigDecimal("10001"), prior));
    }

    private RevenueAllocation allocation() {
        RevenueAllocation allocation = new RevenueAllocation();
        allocation.setRevenueAllocationId(UUID.randomUUID());
        allocation.setCustomerPaidAmount(new BigDecimal("100000.00"));
        allocation.setPlatformFeeAmount(new BigDecimal("5000.00"));
        allocation.setVoucherCommissionAmount(BigDecimal.ZERO);
        allocation.setPartnerPayableAmount(new BigDecimal("95000.00"));
        allocation.setGatewayFeeAmount(BigDecimal.ZERO);
        allocation.setDiscountAmount(BigDecimal.ZERO);
        return allocation;
    }
}
