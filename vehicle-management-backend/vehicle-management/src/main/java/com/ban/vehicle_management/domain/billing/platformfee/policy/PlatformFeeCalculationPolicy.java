package com.ban.vehicle_management.domain.billing.platformfee.policy;

import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeLine;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeRuleVersion;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeBasis;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class PlatformFeeCalculationPolicy {

    public long resolveBasisAmount(PlatformFeeType feeType, long grossAmount, long customerPaidAmount) {
        if (feeType == null || feeType.getFeeBasis() == null) {
            throw new BadRequestException("feeBasis must not be null");
        }
        if (grossAmount < 0 || customerPaidAmount < 0) {
            throw new BadRequestException("Amounts must not be negative");
        }
        if (PlatformFeeBasis.GROSS_AMOUNT.equals(feeType.getFeeBasis())) {
            return grossAmount;
        }
        return customerPaidAmount;
    }

    public long calculateFeeAmount(long basisAmount, BigDecimal feeRate) {
        if (basisAmount < 0) {
            throw new BadRequestException("basisAmount must not be negative");
        }
        if (feeRate == null) {
            throw new BadRequestException("feeRate must not be null");
        }
        return BigDecimal.valueOf(basisAmount)
                .multiply(feeRate)
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    public PlatformFeeLine toFeeLine(PlatformFeeType feeType, PlatformFeeRuleVersion version,
            long grossAmount, long customerPaidAmount) {
        long basisAmount = resolveBasisAmount(feeType, grossAmount, customerPaidAmount);
        long feeAmount = calculateFeeAmount(basisAmount, version.getFeeRate());
        return new PlatformFeeLine(
                feeType.getPlatformFeeTypeId(),
                version.getPlatformFeeRuleVersionId(),
                feeType.getFeeCode(),
                feeType.getName(),
                version.getVersion(),
                version.getFeeRate(),
                feeType.getFeeBasis(),
                feeType.getChargedTo(),
                basisAmount,
                feeAmount);
    }

    public void requireStackableActivation(PlatformFeeType candidate, List<PlatformFeeType> otherActiveTypes) {
        if (candidate == null) {
            throw new BadRequestException("platformFeeType must not be null");
        }
        boolean hasOtherActiveType = otherActiveTypes != null && !otherActiveTypes.isEmpty();
        boolean hasNonStackableActiveType = hasOtherActiveType
                && otherActiveTypes.stream().anyMatch(type -> !type.isStackable());
        if (hasOtherActiveType && (!candidate.isStackable() || hasNonStackableActiveType)) {
            throw new ConflictException("Non-stackable fee type cannot be activated while another fee type is ACTIVE");
        }
    }

    public BigDecimal totalRate(List<BigDecimal> rates) {
        BigDecimal total = BigDecimal.ZERO;
        if (rates == null) {
            return total;
        }
        for (BigDecimal rate : rates) {
            if (rate != null) {
                total = total.add(rate);
            }
        }
        return total;
    }

    public void requireTotalRateWithinLimit(BigDecimal totalRate, BigDecimal maxTotalRate) {
        if (totalRate == null || maxTotalRate == null) {
            throw new BadRequestException("totalRate and maxTotalRate must not be null");
        }
        if (totalRate.compareTo(maxTotalRate) > 0) {
            throw new ConflictException("Total partner fee rate exceeds the configured maximum");
        }
    }
}
