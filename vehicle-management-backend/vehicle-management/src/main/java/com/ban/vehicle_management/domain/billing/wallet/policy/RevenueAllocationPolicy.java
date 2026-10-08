package com.ban.vehicle_management.domain.billing.wallet.policy;

import com.ban.vehicle_management.domain.billing.wallet.model.RevenueAllocation;
import com.ban.vehicle_management.domain.billing.wallet.model.VoucherFinancialTerm;
import com.ban.vehicle_management.shared.enumeration.billing.CommissionBasis;
import com.ban.vehicle_management.shared.enumeration.billing.VoucherFundingSource;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Revenue sharing formulas with VND rounding (HALF_UP, scale 0 for VND whole units,
 * stored as NUMERIC(19,2) with .00).
 */
public class RevenueAllocationPolicy {

    public RevenueAllocation allocate(
            UUID paymentId,
            UUID invoiceId,
            UUID parkingLotId,
            UUID organizationId,
            UUID voucherId,
            BigDecimal grossAmount,
            BigDecimal discountAmount,
            BigDecimal customerPaidAmount,
            BigDecimal platformFeeRate,
            BigDecimal gatewayFee,
            VoucherFinancialTerm term,
            int defaultSettlementDelayDays,
            Instant paidAt) {
        if (grossAmount == null || customerPaidAmount == null || platformFeeRate == null) {
            throw new BadRequestException("Allocation inputs must not be null");
        }
        BigDecimal discount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        BigDecimal gwFee = gatewayFee == null ? BigDecimal.ZERO : gatewayFee;

        BigDecimal platformFee = vnd(customerPaidAmount.multiply(platformFeeRate));

        VoucherFundingSource funding = term == null || term.getFundingSource() == null
                ? VoucherFundingSource.PLATFORM : term.getFundingSource();
        CommissionBasis basis = term == null || term.getCommissionBasis() == null
                ? CommissionBasis.PLATFORM_FEE : term.getCommissionBasis();
        BigDecimal rate = term == null || term.getCommissionRate() == null
                ? BigDecimal.ZERO : term.getCommissionRate();

        BigDecimal basisAmount = switch (basis) {
            case GROSS -> grossAmount;
            case FINAL_AMOUNT -> customerPaidAmount;
            case DISCOUNT_AMOUNT -> discount;
            case PLATFORM_FEE -> platformFee;
        };
        BigDecimal commission = vnd(basisAmount.multiply(rate));
        if (term != null && term.getMaxCommissionAmount() != null
                && commission.compareTo(term.getMaxCommissionAmount()) > 0) {
            commission = vnd(term.getMaxCommissionAmount());
        }

        BigDecimal partnerPayable;
        BigDecimal sponsoredDiscount = BigDecimal.ZERO;
        if (voucherId == null || discount.compareTo(BigDecimal.ZERO) == 0) {
            partnerPayable = customerPaidAmount.subtract(platformFee).subtract(gwFee);
        } else if (funding == VoucherFundingSource.PARTNER) {
            partnerPayable = customerPaidAmount.subtract(platformFee).subtract(gwFee);
        } else if (funding == VoucherFundingSource.PLATFORM) {
            partnerPayable = customerPaidAmount.add(discount).subtract(platformFee).subtract(gwFee);
        } else {
            // External sponsor: receivable, not yet collected -> exclude from partner available.
            sponsoredDiscount = vnd(discount);
            partnerPayable = customerPaidAmount.subtract(platformFee).subtract(gwFee);
        }
        if (partnerPayable.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Partner payable must not be negative");
        }

        int delayDays = term == null ? defaultSettlementDelayDays : term.getSettlementDelayDays();
        Instant availableAt = (paidAt == null ? Instant.now() : paidAt).plus(delayDays, ChronoUnit.DAYS);

        RevenueAllocation allocation = new RevenueAllocation();
        allocation.setRevenueAllocationId(UUID.randomUUID());
        allocation.setPaymentId(paymentId);
        allocation.setInvoiceId(invoiceId);
        allocation.setParkingLotId(parkingLotId);
        allocation.setOrganizationId(organizationId);
        allocation.setVoucherId(voucherId);
        allocation.setGrossAmount(vnd(grossAmount));
        allocation.setDiscountAmount(vnd(discount));
        allocation.setCustomerPaidAmount(vnd(customerPaidAmount));
        allocation.setSponsoredDiscountAmount(vnd(sponsoredDiscount));
        allocation.setPlatformFeeAmount(platformFee);
        allocation.setVoucherCommissionAmount(commission);
        allocation.setPartnerPayableAmount(vnd(partnerPayable));
        allocation.setGatewayFeeAmount(vnd(gwFee));
        allocation.setFundingSourceSnapshot(funding.name());
        allocation.setCommissionRateSnapshot(rate);
        allocation.setCommissionBasisSnapshot(basis.name());
        allocation.setStatus(com.ban.vehicle_management.shared.enumeration.billing.RevenueAllocationStatus.POSTED);
        allocation.setAvailableAt(availableAt);
        allocation.setCreatedAt(Instant.now());
        return allocation;
    }

    private BigDecimal vnd(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.setScale(0, RoundingMode.HALF_UP).setScale(2, RoundingMode.UNNECESSARY);
    }
}
