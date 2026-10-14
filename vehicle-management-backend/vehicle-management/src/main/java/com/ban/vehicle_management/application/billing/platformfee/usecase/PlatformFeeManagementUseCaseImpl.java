package com.ban.vehicle_management.application.billing.platformfee.usecase;

import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.billing.platformfee.authorization.PlatformFeeAccessGuard;
import com.ban.vehicle_management.application.billing.platformfee.config.PlatformFeeProperties;
import com.ban.vehicle_management.application.billing.platformfee.model.result.PlatformFeePageResult;
import com.ban.vehicle_management.application.billing.platformfee.model.result.PlatformFeePreviewResult;
import com.ban.vehicle_management.application.billing.platformfee.model.result.PlatformFeeTypeDetailResult;
import com.ban.vehicle_management.application.billing.platformfee.port.in.PlatformFeeManagementPortIn;
import com.ban.vehicle_management.application.billing.platformfee.port.in.PlatformFeeResolverPortIn;
import com.ban.vehicle_management.application.billing.platformfee.port.out.PlatformFeeRuleVersionPortOut;
import com.ban.vehicle_management.application.billing.platformfee.port.out.PlatformFeeTypePortOut;
import com.ban.vehicle_management.domain.audit.auditlog.model.AuditLog;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeLine;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeResolution;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeRuleVersion;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeType;
import com.ban.vehicle_management.domain.billing.platformfee.policy.PlatformFeeCalculationPolicy;
import com.ban.vehicle_management.domain.billing.platformfee.policy.PlatformFeeRuleVersionPolicy;
import com.ban.vehicle_management.domain.billing.platformfee.policy.PlatformFeeTypePolicy;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeBasis;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeChargedTo;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeRuleVersionStatus;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTypeStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.ConflictException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformFeeManagementUseCaseImpl implements PlatformFeeManagementPortIn {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_KEYWORD_LENGTH = 100;

    private final PlatformFeeTypePortOut feeTypePortOut;
    private final PlatformFeeRuleVersionPortOut ruleVersionPortOut;
    private final PlatformFeeAccessGuard accessGuard;
    private final PlatformFeeResolverPortIn resolverPortIn;
    private final PlatformFeeProperties properties;
    private final AuditLogPortOut auditLogPortOut;
    private final CurrentAccountPortIn currentAccountPortIn;
    private final PlatformFeeTypePolicy typePolicy = new PlatformFeeTypePolicy();
    private final PlatformFeeRuleVersionPolicy versionPolicy = new PlatformFeeRuleVersionPolicy();
    private final PlatformFeeCalculationPolicy calculationPolicy = new PlatformFeeCalculationPolicy();

    public PlatformFeeManagementUseCaseImpl(
            PlatformFeeTypePortOut feeTypePortOut,
            PlatformFeeRuleVersionPortOut ruleVersionPortOut,
            PlatformFeeAccessGuard accessGuard,
            PlatformFeeResolverPortIn resolverPortIn,
            PlatformFeeProperties properties,
            AuditLogPortOut auditLogPortOut,
            CurrentAccountPortIn currentAccountPortIn) {
        this.feeTypePortOut = feeTypePortOut;
        this.ruleVersionPortOut = ruleVersionPortOut;
        this.accessGuard = accessGuard;
        this.resolverPortIn = resolverPortIn;
        this.properties = properties;
        this.auditLogPortOut = auditLogPortOut;
        this.currentAccountPortIn = currentAccountPortIn;
    }

    @Override
    @Transactional
    public PlatformFeeType createFeeType(PlatformFeeType feeType) {
        accessGuard.ensureCanCreate();
        if (feeType == null) {
            throw new BadRequestException("platformFeeType must not be null");
        }
        feeType.setPlatformFeeTypeId(UUID.randomUUID());
        typePolicy.initializeNewType(feeType);
        if (feeTypePortOut.existsByFeeCode(feeType.getFeeCode())) {
            throw new ConflictException("feeCode already exists");
        }
        PlatformFeeType saved = feeTypePortOut.save(feeType);
        writeTypeAudit(saved, "CREATE_FEE_TYPE", null, snapshot(saved));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformFeePageResult searchFeeTypes(String keyword, PlatformFeeTypeStatus status,
            PlatformFeeTransactionType transactionType, PlatformFeeBasis feeBasis, int page, int size) {
        accessGuard.ensureCanRead();
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String normalizedKeyword = normalizeKeyword(keyword);

        return feeTypePortOut.findAll(
                normalizedKeyword, status, transactionType, feeBasis, resolvedPage, resolvedSize);
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformFeeTypeDetailResult getFeeTypeById(UUID feeTypeId) {
        accessGuard.ensureCanRead();
        PlatformFeeType feeType = findTypeOrThrow(feeTypeId);
        Instant now = Instant.now();
        List<PlatformFeeRuleVersion> versions = ruleVersionPortOut.findByFeeTypeId(feeTypeId)
                .stream()
                .sorted(Comparator.comparingInt(PlatformFeeRuleVersion::getVersion))
                .toList();
        PlatformFeeRuleVersion effective = versions.stream()
                .filter(version -> versionPolicy.isEffectiveAt(version, now))
                .max(Comparator.comparingInt(PlatformFeeRuleVersion::getVersion))
                .orElse(null);
        PlatformFeeRuleVersion upcoming = versions.stream()
                .filter(version -> PlatformFeeRuleVersionStatus.ACTIVE.equals(version.getStatus())
                        && version.getEffectiveFrom() != null
                        && version.getEffectiveFrom().isAfter(now))
                .min(Comparator.comparing(PlatformFeeRuleVersion::getEffectiveFrom))
                .orElse(null);
        return new PlatformFeeTypeDetailResult(feeType, effective, upcoming, versions.size(), versions);
    }

    @Override
    @Transactional
    public PlatformFeeType updateDraftFeeType(UUID feeTypeId, PlatformFeeType changes) {
        accessGuard.ensureCanUpdate();
        PlatformFeeType existing = findTypeOrThrow(feeTypeId);
        Map<String, Object> oldData = snapshot(existing);
        typePolicy.updateDraftType(existing, changes);
        PlatformFeeType saved = feeTypePortOut.save(existing);
        writeTypeAudit(saved, "UPDATE_FEE_TYPE", oldData, snapshot(saved));
        return saved;
    }

    @Override
    @Transactional
    public PlatformFeeType activateFeeType(UUID feeTypeId) {
        accessGuard.ensureCanActivate();
        PlatformFeeType feeType = findTypeOrThrow(feeTypeId);
        feeTypePortOut.lockByTransactionType(feeType.getTransactionType());
        feeType = findTypeOrThrow(feeTypeId);
        List<PlatformFeeRuleVersion> resolvableVersions = requireResolvableVersions(feeTypeId);
        requireStackableSatisfied(feeType);
        requireTotalRateWithinLimitForTypeActivation(feeType, resolvableVersions);
        Map<String, Object> oldData = snapshot(feeType);
        typePolicy.activate(feeType);
        PlatformFeeType saved = feeTypePortOut.save(feeType);
        writeTypeAudit(saved, "ACTIVATE_FEE_TYPE", oldData, snapshot(saved));
        return saved;
    }

    @Override
    @Transactional
    public PlatformFeeType deactivateFeeType(UUID feeTypeId) {
        accessGuard.ensureCanActivate();
        PlatformFeeType feeType = findTypeOrThrow(feeTypeId);
        Map<String, Object> oldData = snapshot(feeType);
        typePolicy.deactivate(feeType);
        PlatformFeeType saved = feeTypePortOut.save(feeType);
        writeTypeAudit(saved, "DEACTIVATE_FEE_TYPE", oldData, snapshot(saved));
        return saved;
    }

    @Override
    @Transactional
    public PlatformFeeType archiveFeeType(UUID feeTypeId) {
        accessGuard.ensureCanArchive();
        PlatformFeeType feeType = findTypeOrThrow(feeTypeId);
        Map<String, Object> oldData = snapshot(feeType);
        typePolicy.archive(feeType);
        feeType.setArchivedAt(Instant.now());
        feeType.setArchivedBy(currentAccountPortIn.getCurrentAccountIdOrThrow());
        PlatformFeeType saved = feeTypePortOut.save(feeType);
        writeTypeAudit(saved, "ARCHIVE_FEE_TYPE", oldData, snapshot(saved));
        return saved;
    }

    @Override
    @Transactional
    public PlatformFeeRuleVersion createRuleVersion(UUID feeTypeId, BigDecimal feeRate,
            Instant effectiveFrom, Instant effectiveTo) {
        accessGuard.ensureCanUpdate();
        PlatformFeeType feeType = findTypeOrThrow(feeTypeId);
        if (PlatformFeeTypeStatus.ARCHIVED.equals(feeType.getStatus())) {
            throw new ConflictException("Archived fee type cannot have new versions");
        }
        versionPolicy.validateFeeRate(feeRate);
        versionPolicy.validateEffectivePeriod(effectiveFrom, effectiveTo);
        feeTypePortOut.lockById(feeTypeId);
        PlatformFeeRuleVersion previous = ruleVersionPortOut.findLatestByFeeTypeId(feeTypeId).orElse(null);
        int nextVersion = ruleVersionPortOut.findMaxVersion(feeTypeId) + 1;
        PlatformFeeRuleVersion version = new PlatformFeeRuleVersion();
        version.setPlatformFeeRuleVersionId(UUID.randomUUID());
        version.setPlatformFeeTypeId(feeTypeId);
        version.setFeeRate(feeRate);
        version.setEffectiveFrom(effectiveFrom);
        version.setEffectiveTo(effectiveTo);
        versionPolicy.initializeNewVersion(version, nextVersion, previous);
        PlatformFeeRuleVersion saved = ruleVersionPortOut.save(version);
        writeVersionAudit(saved, "CREATE_FEE_RULE_VERSION", null, snapshot(saved));
        return saved;
    }

    @Override
    @Transactional
    public PlatformFeeRuleVersion updateDraftRuleVersion(UUID feeTypeId, UUID versionId,
            BigDecimal feeRate, Instant effectiveFrom, Instant effectiveTo) {
        accessGuard.ensureCanUpdate();
        PlatformFeeRuleVersion existing = findVersionOrThrow(feeTypeId, versionId);
        Map<String, Object> oldData = snapshot(existing);
        versionPolicy.updateDraftVersion(existing, feeRate, effectiveFrom, effectiveTo);
        PlatformFeeRuleVersion saved = ruleVersionPortOut.save(existing);
        writeVersionAudit(saved, "UPDATE_FEE_RULE_VERSION", oldData, snapshot(saved));
        return saved;
    }

    @Override
    @Transactional
    public PlatformFeeRuleVersion activateRuleVersion(UUID feeTypeId, UUID versionId) {
        accessGuard.ensureCanActivate();
        PlatformFeeType feeType = findTypeOrThrow(feeTypeId);
        feeTypePortOut.lockByTransactionType(feeType.getTransactionType());
        PlatformFeeRuleVersion version = findVersionOrThrow(feeTypeId, versionId);
        List<PlatformFeeRuleVersion> siblings = ruleVersionPortOut.findActiveByFeeTypeId(feeTypeId);
        Map<String, Object> oldData = snapshot(version);
        versionPolicy.activate(version, siblings);
        requireTotalRateWithinLimitForVersionActivation(feeTypeId, version);
        version.setActivatedAt(Instant.now());
        version.setActivatedBy(currentAccountPortIn.getCurrentAccountIdOrThrow());
        version.setDeactivatedAt(null);
        version.setDeactivatedBy(null);
        PlatformFeeRuleVersion saved = ruleVersionPortOut.save(version);
        writeVersionAudit(saved, "ACTIVATE_FEE_RULE_VERSION", oldData, snapshot(saved));
        return saved;
    }

    @Override
    @Transactional
    public PlatformFeeRuleVersion deactivateRuleVersion(UUID feeTypeId, UUID versionId) {
        accessGuard.ensureCanActivate();
        findTypeOrThrow(feeTypeId);
        PlatformFeeRuleVersion version = findVersionOrThrow(feeTypeId, versionId);
        Map<String, Object> oldData = snapshot(version);
        versionPolicy.deactivate(version);
        version.setDeactivatedAt(Instant.now());
        version.setDeactivatedBy(currentAccountPortIn.getCurrentAccountIdOrThrow());
        PlatformFeeRuleVersion saved = ruleVersionPortOut.save(version);
        writeVersionAudit(saved, "DEACTIVATE_FEE_RULE_VERSION", oldData, snapshot(saved));
        return saved;
    }

    @Override
    @Transactional
    public PlatformFeeRuleVersion archiveRuleVersion(UUID feeTypeId, UUID versionId) {
        accessGuard.ensureCanArchive();
        findTypeOrThrow(feeTypeId);
        PlatformFeeRuleVersion version = findVersionOrThrow(feeTypeId, versionId);
        Map<String, Object> oldData = snapshot(version);
        versionPolicy.archive(version);
        version.setArchivedAt(Instant.now());
        version.setArchivedBy(currentAccountPortIn.getCurrentAccountIdOrThrow());
        PlatformFeeRuleVersion saved = ruleVersionPortOut.save(version);
        writeVersionAudit(saved, "ARCHIVE_FEE_RULE_VERSION", oldData, snapshot(saved));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlatformFeeRuleVersion> getVersionHistory(UUID feeTypeId) {
        accessGuard.ensureCanRead();
        findTypeOrThrow(feeTypeId);
        return ruleVersionPortOut.findByFeeTypeId(feeTypeId)
                .stream()
                .sorted(Comparator.comparingInt(PlatformFeeRuleVersion::getVersion))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformFeePreviewResult previewFees(PlatformFeeTransactionType transactionType,
            Instant effectiveAt, long grossAmount, long customerPaidAmount) {
        accessGuard.ensureCanPreview();
        if (transactionType == null) {
            throw new BadRequestException("transactionType must not be null");
        }
        if (grossAmount < 0 || customerPaidAmount < 0) {
            throw new BadRequestException("Amounts must not be negative");
        }
        PlatformFeeResolution resolution =
                resolverPortIn.resolveFees(transactionType, effectiveAt, grossAmount, customerPaidAmount);
        List<PlatformFeePreviewResult.PlatformFeePreviewLineResult> lines = resolution.getFeeLines()
                .stream()
                .map(this::toPreviewLine)
                .toList();
        long partnerRemaining = Math.max(0L, customerPaidAmount - resolution.getTotalFeeAmount());
        return new PlatformFeePreviewResult(
                resolution.getTransactionType(),
                resolution.getEffectiveAt(),
                resolution.getGrossAmount(),
                resolution.getCustomerPaidAmount(),
                lines,
                resolution.getTotalFeeAmount(),
                resolution.getEffectiveTotalRate(),
                partnerRemaining);
    }

    private PlatformFeePreviewResult.PlatformFeePreviewLineResult toPreviewLine(PlatformFeeLine line) {
        return new PlatformFeePreviewResult.PlatformFeePreviewLineResult(
                line.getFeeTypeId(),
                line.getRuleVersionId(),
                line.getFeeCode(),
                line.getFeeName(),
                line.getVersion(),
                line.getFeeRate(),
                line.getFeeBasis() == null ? null : line.getFeeBasis().name(),
                line.getChargedTo() == null ? null : line.getChargedTo().name(),
                line.getBasisAmount(),
                line.getFeeAmount());
    }

    private PlatformFeeType findTypeOrThrow(UUID feeTypeId) {
        if (feeTypeId == null) {
            throw new BadRequestException("feeTypeId must not be null");
        }
        return feeTypePortOut.findById(feeTypeId)
                .orElseThrow(() -> new NotFoundException("Platform fee type not found"));
    }

    private PlatformFeeRuleVersion findVersionOrThrow(UUID feeTypeId, UUID versionId) {
        if (versionId == null) {
            throw new BadRequestException("versionId must not be null");
        }
        PlatformFeeRuleVersion version = ruleVersionPortOut.findById(versionId)
                .orElseThrow(() -> new NotFoundException("Platform fee rule version not found"));
        if (feeTypeId != null && !feeTypeId.equals(version.getPlatformFeeTypeId())) {
            throw new NotFoundException("Platform fee rule version not found");
        }
        return version;
    }

    private List<PlatformFeeRuleVersion> requireResolvableVersions(UUID feeTypeId) {
        Instant now = Instant.now();
        List<PlatformFeeRuleVersion> activeVersions = ruleVersionPortOut.findActiveByFeeTypeId(feeTypeId);
        List<PlatformFeeRuleVersion> resolvable = activeVersions == null ? List.of() : activeVersions.stream()
                .filter(version -> version.getEffectiveFrom() != null)
                .filter(version -> version.getEffectiveTo() == null || version.getEffectiveTo().isAfter(now))
                .toList();
        if (resolvable.isEmpty()) {
            throw new ConflictException("Fee type has no current or future ACTIVE rule version to activate with");
        }
        return resolvable;
    }

    private void requireStackableSatisfied(PlatformFeeType candidate) {
        List<PlatformFeeType> others = feeTypePortOut
                .findActiveByTransactionType(candidate.getTransactionType())
                .stream()
                .filter(type -> !type.getPlatformFeeTypeId().equals(candidate.getPlatformFeeTypeId()))
                .toList();
        calculationPolicy.requireStackableActivation(candidate, others);
    }

    private void requireTotalRateWithinLimitForTypeActivation(
            PlatformFeeType candidate, List<PlatformFeeRuleVersion> candidateVersions) {
        for (PlatformFeeRuleVersion version : candidateVersions) {
            requireTotalRateWithinLimitAcrossPeriod(candidate, version);
        }
    }

    private void requireTotalRateWithinLimitForVersionActivation(UUID feeTypeId, PlatformFeeRuleVersion candidate) {
        PlatformFeeType candidateType = findTypeOrThrow(feeTypeId);
        requireTotalRateWithinLimitAcrossPeriod(candidateType, candidate);
    }

    private void requireTotalRateWithinLimitAcrossPeriod(
            PlatformFeeType candidateType, PlatformFeeRuleVersion candidateVersion) {
        if (!PlatformFeeChargedTo.PARTNER.equals(candidateType.getChargedTo())) {
            return;
        }
        Instant periodStart = candidateVersion.getEffectiveFrom();
        Instant periodEnd = candidateVersion.getEffectiveTo();
        TreeSet<Instant> checkpoints = new TreeSet<>();
        checkpoints.add(periodStart);

        List<PlatformFeeType> others = feeTypePortOut
                .findActiveByTransactionType(candidateType.getTransactionType())
                .stream()
                .filter(type -> !type.getPlatformFeeTypeId().equals(candidateType.getPlatformFeeTypeId()))
                .filter(type -> PlatformFeeChargedTo.PARTNER.equals(type.getChargedTo()))
                .toList();

        for (PlatformFeeType other : others) {
            for (PlatformFeeRuleVersion version : ruleVersionPortOut
                    .findActiveByFeeTypeId(other.getPlatformFeeTypeId())) {
                addBoundaryIfInside(checkpoints, version.getEffectiveFrom(), periodStart, periodEnd);
                addBoundaryIfInside(checkpoints, version.getEffectiveTo(), periodStart, periodEnd);
            }
        }

        for (Instant checkpoint : checkpoints) {
            List<BigDecimal> rates = new java.util.ArrayList<>();
            rates.add(candidateVersion.getFeeRate());
            for (PlatformFeeType other : others) {
                ruleVersionPortOut.findEffectiveVersion(other.getPlatformFeeTypeId(), checkpoint)
                        .map(PlatformFeeRuleVersion::getFeeRate)
                        .ifPresent(rates::add);
            }
            calculationPolicy.requireTotalRateWithinLimit(
                    calculationPolicy.totalRate(rates), properties.maxPartnerTotalRate());
        }
    }

    private void addBoundaryIfInside(
            TreeSet<Instant> checkpoints, Instant boundary, Instant periodStart, Instant periodEnd) {
        if (boundary != null && !boundary.isBefore(periodStart)
                && (periodEnd == null || boundary.isBefore(periodEnd))) {
            checkpoints.add(boundary);
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String normalized = keyword.trim();
        if (normalized.length() > MAX_KEYWORD_LENGTH) {
            throw new BadRequestException("keyword must not exceed 100 characters");
        }
        return normalized;
    }

    private Map<String, Object> snapshot(PlatformFeeType feeType) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("feeCode", text(feeType.getFeeCode()));
        values.put("name", text(feeType.getName()));
        values.put("transactionType", text(feeType.getTransactionType()));
        values.put("feeBasis", text(feeType.getFeeBasis()));
        values.put("chargedTo", text(feeType.getChargedTo()));
        values.put("stackable", String.valueOf(feeType.isStackable()));
        values.put("status", text(feeType.getStatus()));
        return values;
    }

    private Map<String, Object> snapshot(PlatformFeeRuleVersion version) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("feeTypeId", text(version.getPlatformFeeTypeId()));
        values.put("version", String.valueOf(version.getVersion()));
        values.put("feeRate", text(version.getFeeRate()));
        values.put("effectiveFrom", text(version.getEffectiveFrom()));
        values.put("effectiveTo", text(version.getEffectiveTo()));
        values.put("status", text(version.getStatus()));
        return values;
    }

    private String text(Object value) {
        return value == null ? "" : value.toString();
    }

    private void writeTypeAudit(PlatformFeeType feeType, String action,
            Map<String, Object> oldData, Map<String, Object> newData) {
        writeAudit(action, "platform_fee_types", feeType.getPlatformFeeTypeId(), oldData, newData);
    }

    private void writeVersionAudit(PlatformFeeRuleVersion version, String action,
            Map<String, Object> oldData, Map<String, Object> newData) {
        writeAudit(action, "platform_fee_rule_versions", version.getPlatformFeeRuleVersionId(), oldData, newData);
    }

    private void writeAudit(String action, String targetTable, UUID targetId,
            Map<String, Object> oldData, Map<String, Object> newData) {
        if (auditLogPortOut == null || currentAccountPortIn == null) {
            return;
        }
        AuditLog auditLog = new AuditLog();
        auditLog.setAuditLogId(UUID.randomUUID());
        auditLog.setActorAccountId(currentAccountPortIn.getCurrentAccountIdOrThrow());
        auditLog.setAction(action);
        auditLog.setTargetSchema("billing");
        auditLog.setTargetTable(targetTable);
        auditLog.setTargetId(targetId);
        auditLog.setOldData(oldData);
        auditLog.setNewData(newData);
        auditLogPortOut.save(auditLog);
    }
}
