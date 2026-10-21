package com.ban.vehicle_management.domain.billing.platformfee.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeBasis;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeCalculationType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeChargedTo;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTypeStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlatformFeeCalculationPolicyTest {

    private final PlatformFeeCalculationPolicy policy = new PlatformFeeCalculationPolicy();

    @Test
    void shouldRoundHalfUpToVnd() {
        assertEquals(1L, policy.calculateFeeAmount(1L, new BigDecimal("0.50")));
        assertEquals(1L, policy.calculateFeeAmount(3L, new BigDecimal("0.333333")));
        assertEquals(100000L, policy.calculateFeeAmount(1000000L, new BigDecimal("0.10")));
    }

    @Test
    void shouldUseGrossAmountBasis() {
        PlatformFeeType type = feeType(PlatformFeeBasis.GROSS_AMOUNT);

        assertEquals(1000000L, policy.resolveBasisAmount(type, 1000000L, 900000L));
    }

    @Test
    void shouldUseAfterDiscountBasis() {
        PlatformFeeType type = feeType(PlatformFeeBasis.AFTER_DISCOUNT);

        assertEquals(900000L, policy.resolveBasisAmount(type, 1000000L, 900000L));
    }

    @Test
    void shouldRejectTotalRateAboveLimit() {
        assertThrows(ConflictException.class, () -> policy.requireTotalRateWithinLimit(
                new BigDecimal("0.26"), new BigDecimal("0.25")));
    }

    @Test
    void shouldAllowTotalRateAtLimit() {
        policy.requireTotalRateWithinLimit(
                new BigDecimal("0.25"), new BigDecimal("0.25"));
    }

    @Test
    void shouldRejectNonStackableWhenOthersActive() {
        PlatformFeeType candidate = feeType(PlatformFeeBasis.GROSS_AMOUNT);
        candidate.setStackable(false);

        assertThrows(ConflictException.class,
                () -> policy.requireStackableActivation(candidate, List.of(feeType(PlatformFeeBasis.GROSS_AMOUNT))));
    }

    @Test
    void shouldRejectStackableCandidateWhenExistingFeeIsNonStackable() {
        PlatformFeeType candidate = feeType(PlatformFeeBasis.GROSS_AMOUNT);
        PlatformFeeType existing = feeType(PlatformFeeBasis.GROSS_AMOUNT);
        existing.setStackable(false);

        assertThrows(ConflictException.class,
                () -> policy.requireStackableActivation(candidate, List.of(existing)));
    }

    private PlatformFeeType feeType(PlatformFeeBasis basis) {
        PlatformFeeType type = new PlatformFeeType();
        type.setPlatformFeeTypeId(UUID.randomUUID());
        type.setFeeCode("PLATFORM_COMMISSION");
        type.setName("Phi nen tang");
        type.setTransactionType(PlatformFeeTransactionType.SUBSCRIPTION_PURCHASE);
        type.setCalculationType(PlatformFeeCalculationType.PERCENTAGE);
        type.setFeeBasis(basis);
        type.setChargedTo(PlatformFeeChargedTo.PARTNER);
        type.setStackable(true);
        type.setStatus(PlatformFeeTypeStatus.ACTIVE);
        return type;
    }
}
