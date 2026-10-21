package com.ban.vehicle_management.domain.billing.platformfee.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeRuleVersion;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeRuleVersionStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlatformFeeRuleVersionPolicyTest {

    private final PlatformFeeRuleVersionPolicy policy = new PlatformFeeRuleVersionPolicy();

    @Test
    void shouldRejectNegativeFeeRate() {
        assertThrows(BadRequestException.class,
                () -> policy.validateFeeRate(new BigDecimal("-0.01")));
    }

    @Test
    void shouldRejectFeeRateGreaterThanOne() {
        assertThrows(BadRequestException.class,
                () -> policy.validateFeeRate(new BigDecimal("1.000001")));
    }

    @Test
    void shouldRejectEffectiveToBeforeEffectiveFrom() {
        Instant from = Instant.parse("2026-11-01T00:00:00Z");
        Instant to = Instant.parse("2026-10-01T00:00:00Z");

        assertThrows(BadRequestException.class, () -> policy.validateEffectivePeriod(from, to));
    }

    @Test
    void shouldRejectOverlappingActiveSibling() {
        PlatformFeeRuleVersion candidate = version(PlatformFeeRuleVersionStatus.DRAFT,
                Instant.parse("2026-11-01T00:00:00Z"), null);
        PlatformFeeRuleVersion sibling = version(PlatformFeeRuleVersionStatus.ACTIVE,
                Instant.parse("2026-10-01T00:00:00Z"), null);

        assertThrows(ConflictException.class, () -> policy.requireNoOverlap(candidate, List.of(sibling)));
    }

    @Test
    void shouldAllowAdjacentPeriods() {
        PlatformFeeRuleVersion candidate = version(PlatformFeeRuleVersionStatus.DRAFT,
                Instant.parse("2026-11-01T00:00:00Z"), null);
        PlatformFeeRuleVersion sibling = version(PlatformFeeRuleVersionStatus.ACTIVE,
                Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-11-01T00:00:00Z"));

        policy.requireNoOverlap(candidate, List.of(sibling));
    }

    @Test
    void shouldMatchEffectiveAtSemantics() {
        PlatformFeeRuleVersion version = version(PlatformFeeRuleVersionStatus.ACTIVE,
                Instant.parse("2026-11-01T00:00:00Z"), Instant.parse("2026-12-01T00:00:00Z"));

        assertFalse(policy.isEffectiveAt(version, Instant.parse("2026-10-15T00:00:00Z")));
        assertTrue(policy.isEffectiveAt(version, Instant.parse("2026-11-15T00:00:00Z")));
        assertFalse(policy.isEffectiveAt(version, Instant.parse("2026-12-01T00:00:00Z")));
    }

    @Test
    void shouldRejectActivateWithoutEffectiveFrom() {
        PlatformFeeRuleVersion version = version(PlatformFeeRuleVersionStatus.DRAFT, null, null);

        assertThrows(BadRequestException.class, () -> policy.activate(version, List.of()));
    }

    @Test
    void shouldRejectUpdateWhenActive() {
        PlatformFeeRuleVersion version = version(PlatformFeeRuleVersionStatus.ACTIVE,
                Instant.parse("2026-11-01T00:00:00Z"), null);

        assertThrows(ConflictException.class, () -> policy.updateDraftVersion(
                version, new BigDecimal("0.10"), version.getEffectiveFrom(), null));
    }

    @Test
    void shouldAssignNextVersionAndPreviousLink() {
        PlatformFeeRuleVersion previous = version(PlatformFeeRuleVersionStatus.ACTIVE,
                Instant.parse("2026-10-01T00:00:00Z"), null);
        PlatformFeeRuleVersion next = new PlatformFeeRuleVersion();
        next.setPlatformFeeRuleVersionId(UUID.randomUUID());
        next.setPlatformFeeTypeId(UUID.randomUUID());
        next.setFeeRate(new BigDecimal("0.10"));
        next.setEffectiveFrom(Instant.parse("2026-11-01T00:00:00Z"));

        policy.initializeNewVersion(next, 2, previous);

        assertEquals(2, next.getVersion());
        assertEquals(previous.getPlatformFeeRuleVersionId(), next.getPreviousVersionId());
        assertEquals(PlatformFeeRuleVersionStatus.DRAFT, next.getStatus());
    }

    private PlatformFeeRuleVersion version(PlatformFeeRuleVersionStatus status, Instant from, Instant to) {
        PlatformFeeRuleVersion version = new PlatformFeeRuleVersion();
        version.setPlatformFeeRuleVersionId(UUID.randomUUID());
        version.setPlatformFeeTypeId(UUID.randomUUID());
        version.setVersion(1);
        version.setFeeRate(new BigDecimal("0.10"));
        version.setEffectiveFrom(from);
        version.setEffectiveTo(to);
        version.setStatus(status);
        return version;
    }
}
