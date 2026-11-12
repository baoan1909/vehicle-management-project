package com.ban.vehicle_management.domain.billing.wallet.policy;

import com.ban.vehicle_management.domain.billing.wallet.model.RevenueAllocation;
import com.ban.vehicle_management.domain.billing.wallet.model.RevenueAllocationReversal;
import com.ban.vehicle_management.domain.billing.wallet.model.RevenueAllocationReversalTotals;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

public class RevenueAllocationReversalPolicy {

    public RevenueAllocationReversal create(
            RevenueAllocation allocation,
            UUID refundPaymentId,
            BigDecimal refundAmount,
            RevenueAllocationReversalTotals reversed) {
        if (allocation == null || refundPaymentId == null || refundAmount == null
                || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Valid allocation, refund payment and refund amount are required");
        }
        BigDecimal originalPaid = amount(allocation.getCustomerPaidAmount());
        BigDecimal totalRefunded = amount(reversed.refundAmount()).add(refundAmount);
        if (originalPaid.compareTo(BigDecimal.ZERO) <= 0 || totalRefunded.compareTo(originalPaid) > 0) {
            throw new BadRequestException("Allocation reversal exceeds original customer payment");
        }
        boolean finalReversal = totalRefunded.compareTo(originalPaid) == 0;
        BigDecimal ratio = refundAmount.divide(originalPaid, 10, RoundingMode.HALF_UP);

        RevenueAllocationReversal reversal = new RevenueAllocationReversal();
        reversal.setRevenueAllocationReversalId(UUID.randomUUID());
        reversal.setRevenueAllocationId(allocation.getRevenueAllocationId());
        reversal.setRefundPaymentId(refundPaymentId);
        reversal.setRefundAmount(vnd(refundAmount));
        reversal.setReversalRatio(ratio);
        reversal.setPlatformFeeAmount(component(
                allocation.getPlatformFeeAmount(), reversed.platformFeeAmount(), ratio, finalReversal));
        reversal.setVoucherCommissionAmount(component(
                allocation.getVoucherCommissionAmount(), reversed.voucherCommissionAmount(), ratio, finalReversal));
        reversal.setPartnerPayableAmount(component(
                allocation.getPartnerPayableAmount(), reversed.partnerPayableAmount(), ratio, finalReversal));
        reversal.setGatewayFeeAmount(component(
                allocation.getGatewayFeeAmount(), reversed.gatewayFeeAmount(), ratio, finalReversal));
        reversal.setDiscountAmount(component(
                allocation.getDiscountAmount(), reversed.discountAmount(), ratio, finalReversal));
        reversal.setCreatedAt(Instant.now());
        return reversal;
    }

    private BigDecimal component(BigDecimal original, BigDecimal alreadyReversed,
            BigDecimal ratio, boolean finalReversal) {
        BigDecimal source = amount(original);
        BigDecimal prior = amount(alreadyReversed);
        BigDecimal value = finalReversal ? source.subtract(prior) : vnd(source.multiply(ratio));
        if (value.compareTo(BigDecimal.ZERO) < 0 || prior.add(value).compareTo(source) > 0) {
            throw new BadRequestException("Allocation component reversal exceeds original amount");
        }
        return vnd(value);
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal vnd(BigDecimal value) {
        return amount(value).setScale(0, RoundingMode.HALF_UP).setScale(2, RoundingMode.UNNECESSARY);
    }
}
