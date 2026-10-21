package com.ban.vehicle_management.domain.billing.platformfee.policy;

import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeRuleVersion;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeRuleVersionStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class PlatformFeeRuleVersionPolicy {

    public void initializeNewVersion(PlatformFeeRuleVersion version, int nextVersion, PlatformFeeRuleVersion previous) {
        requireVersion(version);
        if (nextVersion <= 0) {
            throw new BadRequestException("version must be greater than 0");
        }
        version.setVersion(nextVersion);
        version.setPreviousVersionId(previous == null ? null : previous.getPlatformFeeRuleVersionId());
        version.setStatus(PlatformFeeRuleVersionStatus.DRAFT);
        version.setActivatedAt(null);
        version.setActivatedBy(null);
        version.setDeactivatedAt(null);
        version.setDeactivatedBy(null);
        version.setArchivedAt(null);
        version.setArchivedBy(null);
        validateFeeRate(version.getFeeRate());
        validateEffectivePeriod(version.getEffectiveFrom(), version.getEffectiveTo());
        validateState(version);
    }

    public void updateDraftVersion(PlatformFeeRuleVersion existing, BigDecimal feeRate,
            Instant effectiveFrom, Instant effectiveTo) {
        requireVersion(existing);
        requireStatus(existing, PlatformFeeRuleVersionStatus.DRAFT, "Only DRAFT rule version can be updated");
        validateFeeRate(feeRate);
        validateEffectivePeriod(effectiveFrom, effectiveTo);
        existing.setFeeRate(feeRate);
        existing.setEffectiveFrom(effectiveFrom);
        existing.setEffectiveTo(effectiveTo);
        validateState(existing);
    }

    public void activate(PlatformFeeRuleVersion version, List<PlatformFeeRuleVersion> activeSiblings) {
        requireVersion(version);
        if (PlatformFeeRuleVersionStatus.ARCHIVED.equals(version.getStatus())) {
            throw new ConflictException("Archived rule version cannot be activated");
        }
        if (PlatformFeeRuleVersionStatus.ACTIVE.equals(version.getStatus())) {
            return;
        }
        if (!PlatformFeeRuleVersionStatus.DRAFT.equals(version.getStatus())
                && !PlatformFeeRuleVersionStatus.INACTIVE.equals(version.getStatus())) {
            throw new ConflictException("Only DRAFT or INACTIVE rule version can be activated");
        }
        if (version.getEffectiveFrom() == null) {
            throw new BadRequestException("effectiveFrom is required to activate a rule version");
        }
        validateEffectivePeriod(version.getEffectiveFrom(), version.getEffectiveTo());
        requireNoOverlap(version, activeSiblings);
        version.setStatus(PlatformFeeRuleVersionStatus.ACTIVE);
        validateState(version);
    }

    public void deactivate(PlatformFeeRuleVersion version) {
        requireVersion(version);
        if (!PlatformFeeRuleVersionStatus.ACTIVE.equals(version.getStatus())) {
            throw new ConflictException("Only ACTIVE rule version can be deactivated");
        }
        version.setStatus(PlatformFeeRuleVersionStatus.INACTIVE);
        validateState(version);
    }

    public void archive(PlatformFeeRuleVersion version) {
        requireVersion(version);
        if (PlatformFeeRuleVersionStatus.ARCHIVED.equals(version.getStatus())) {
            return;
        }
        if (!PlatformFeeRuleVersionStatus.DRAFT.equals(version.getStatus())
                && !PlatformFeeRuleVersionStatus.INACTIVE.equals(version.getStatus())) {
            throw new ConflictException("Only DRAFT or INACTIVE rule version can be archived");
        }
        version.setStatus(PlatformFeeRuleVersionStatus.ARCHIVED);
        validateState(version);
    }

    public boolean isEffectiveAt(PlatformFeeRuleVersion version, Instant effectiveAt) {
        if (version == null || effectiveAt == null) {
            return false;
        }
        if (!PlatformFeeRuleVersionStatus.ACTIVE.equals(version.getStatus())) {
            return false;
        }
        if (version.getEffectiveFrom() != null && effectiveAt.isBefore(version.getEffectiveFrom())) {
            return false;
        }
        return version.getEffectiveTo() == null || effectiveAt.isBefore(version.getEffectiveTo());
    }

    public void requireNoOverlap(PlatformFeeRuleVersion candidate, List<PlatformFeeRuleVersion> activeSiblings) {
        if (candidate == null || activeSiblings == null) {
            return;
        }
        for (PlatformFeeRuleVersion sibling : activeSiblings) {
            if (sibling == null || !PlatformFeeRuleVersionStatus.ACTIVE.equals(sibling.getStatus())) {
                continue;
            }
            if (candidate.getPlatformFeeRuleVersionId() != null
                    && candidate.getPlatformFeeRuleVersionId().equals(sibling.getPlatformFeeRuleVersionId())) {
                continue;
            }
            if (periodsOverlap(candidate.getEffectiveFrom(), candidate.getEffectiveTo(),
                    sibling.getEffectiveFrom(), sibling.getEffectiveTo())) {
                throw new ConflictException("Rule version effective period overlaps an active version");
            }
        }
    }

    public void validateFeeRate(BigDecimal feeRate) {
        if (feeRate == null) {
            throw new BadRequestException("feeRate must not be null");
        }
        if (feeRate.compareTo(BigDecimal.ZERO) < 0 || feeRate.compareTo(BigDecimal.ONE) > 0) {
            throw new BadRequestException("feeRate must be between 0 and 1");
        }
    }

    public void validateEffectivePeriod(Instant effectiveFrom, Instant effectiveTo) {
        if (effectiveFrom != null && effectiveTo != null && !effectiveTo.isAfter(effectiveFrom)) {
            throw new BadRequestException("effectiveTo must be after effectiveFrom");
        }
    }

    public void validateState(PlatformFeeRuleVersion version) {
        requireVersion(version);
        requireField(version.getPlatformFeeRuleVersionId(), "platformFeeRuleVersionId");
        requireField(version.getPlatformFeeTypeId(), "platformFeeTypeId");
        if (version.getVersion() <= 0) {
            throw new BadRequestException("version must be greater than 0");
        }
        requireField(version.getFeeRate(), "feeRate");
        requireField(version.getStatus(), "status");
        validateFeeRate(version.getFeeRate());
        validateEffectivePeriod(version.getEffectiveFrom(), version.getEffectiveTo());
    }

    private boolean periodsOverlap(Instant startA, Instant endA, Instant startB, Instant endB) {
        Instant resolvedStartA = startA == null ? Instant.MIN : startA;
        Instant resolvedEndA = endA == null ? Instant.MAX : endA;
        Instant resolvedStartB = startB == null ? Instant.MIN : startB;
        Instant resolvedEndB = endB == null ? Instant.MAX : endB;
        return resolvedStartA.isBefore(resolvedEndB) && resolvedStartB.isBefore(resolvedEndA);
    }

    private void requireStatus(PlatformFeeRuleVersion version, PlatformFeeRuleVersionStatus expected, String message) {
        if (!expected.equals(version.getStatus())) {
            throw new ConflictException(message);
        }
    }

    private void requireVersion(PlatformFeeRuleVersion version) {
        requireField(version, "platformFeeRuleVersion");
    }

    private void requireField(Object value, String fieldName) {
        if (value == null) {
            throw new BadRequestException(fieldName + " must not be null");
        }
    }
}
