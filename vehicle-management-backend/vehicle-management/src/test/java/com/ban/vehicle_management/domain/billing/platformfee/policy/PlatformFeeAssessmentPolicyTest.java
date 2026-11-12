package com.ban.vehicle_management.domain.billing.platformfee.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeLine;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeResolution;
import com.ban.vehicle_management.shared.enumeration.billing.FundCustody;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeAssessmentStatus;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeBasis;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeChargedTo;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeScopeType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlatformFeeAssessmentPolicyTest {
    private final PlatformFeeAssessmentPolicy policy = new PlatformFeeAssessmentPolicy();

    @Test
    void snapshotsResolvedRuleAndCustody() {
        PlatformFeeLine line = new PlatformFeeLine(
                UUID.randomUUID(), UUID.randomUUID(), "GLOBAL_FEE", "Global fee", 3,
                new BigDecimal("0.05"), PlatformFeeBasis.AFTER_DISCOUNT,
                PlatformFeeChargedTo.PARTNER, 100000L, 5000L);
        PlatformFeeResolution resolution = new PlatformFeeResolution(
                PlatformFeeTransactionType.PARKING_SESSION, PlatformFeeScopeType.GLOBAL,
                true, true, Instant.parse("2026-11-10T00:00:00Z"),
                120000L, 100000L, List.of(line), 5000L, new BigDecimal("0.05"));

        var assessment = policy.createApplied(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                FundCustody.PARTNER, resolution, Instant.parse("2026-11-10T00:01:00Z"));

        assertEquals(PlatformFeeAssessmentStatus.APPLIED, assessment.getStatus());
        assertEquals(PlatformFeeScopeType.GLOBAL, assessment.getResolvedScope());
        assertEquals(FundCustody.PARTNER, assessment.getFundCustody());
        assertEquals(new BigDecimal("5000.00"), assessment.getTotalFeeAmount());
        assertEquals(1, assessment.getLines().size());
        assertEquals(null, assessment.getLines().get(0).getTransactionType());
        assertNotNull(assessment.getAmountFingerprint());
        assertEquals(64, assessment.getAmountFingerprint().length());
    }

    @Test
    void rejectsMissingConfigurationInsteadOfSilentlyChargingZero() {
        PlatformFeeResolution resolution = new PlatformFeeResolution(
                PlatformFeeTransactionType.MANUAL, null, false, false,
                Instant.now(), 100000L, 100000L, List.of(), 0L, BigDecimal.ZERO);

        assertThrows(ConflictException.class, () -> policy.createApplied(
                UUID.randomUUID(), UUID.randomUUID(), null,
                FundCustody.PARTNER, resolution, Instant.now()));
    }
}
