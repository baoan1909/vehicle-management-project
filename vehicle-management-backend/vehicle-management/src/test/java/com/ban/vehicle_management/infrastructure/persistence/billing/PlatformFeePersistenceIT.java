package com.ban.vehicle_management.infrastructure.persistence.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.application.billing.platformfee.model.result.PlatformFeePageResult;
import com.ban.vehicle_management.application.billing.platformfee.port.out.PlatformFeeRuleVersionPortOut;
import com.ban.vehicle_management.application.billing.platformfee.port.out.PlatformFeeTypePortOut;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeRuleVersion;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeBasis;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeCalculationType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeChargedTo;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeRuleVersionStatus;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTypeStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PlatformFeePersistenceIT extends com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport {

    @Autowired
    private PlatformFeeTypePortOut feeTypePortOut;

    @Autowired
    private PlatformFeeRuleVersionPortOut ruleVersionPortOut;

    @Test
    void shouldRunMigrationAndPersistFeeTypeWithVersions() {
        PlatformFeeType feeType = feeType("PLATFORM_COMMISSION");
        PlatformFeeType saved = feeTypePortOut.save(feeType);

        PlatformFeeRuleVersion v1 = version(saved.getPlatformFeeTypeId(), 1, new BigDecimal("0.05"),
                Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-11-01T00:00:00Z"));
        ruleVersionPortOut.save(v1);
        PlatformFeeRuleVersion v2 = version(saved.getPlatformFeeTypeId(), 2, new BigDecimal("0.10"),
                Instant.parse("2026-11-01T00:00:00Z"), null);
        v2.setStatus(PlatformFeeRuleVersionStatus.ACTIVE);
        ruleVersionPortOut.save(v2);

        assertTrue(feeTypePortOut.findByFeeCode("PLATFORM_COMMISSION").isPresent());
        assertEquals(2, ruleVersionPortOut.findByFeeTypeId(saved.getPlatformFeeTypeId()).size());

        assertTrue(ruleVersionPortOut
                .findEffectiveVersion(saved.getPlatformFeeTypeId(), Instant.parse("2026-10-15T00:00:00Z"))
                .isEmpty());
        assertTrue(ruleVersionPortOut
                .findEffectiveVersion(saved.getPlatformFeeTypeId(), Instant.parse("2026-11-15T00:00:00Z"))
                .isPresent());
        assertEquals(2, ruleVersionPortOut.findMaxVersion(saved.getPlatformFeeTypeId()));
    }

    @Test
    void shouldRejectDuplicateFeeCode() {
        feeTypePortOut.save(feeType("OPERATION_FEE"));

        assertThrows(DataIntegrityViolationException.class, () -> feeTypePortOut.save(feeType("OPERATION_FEE")));
    }

    @Test
    void shouldRejectDuplicateTypeVersion() {
        PlatformFeeType saved = feeTypePortOut.save(feeType("DUPLICATE_VERSION_FEE"));
        ruleVersionPortOut.save(version(saved.getPlatformFeeTypeId(), 1, new BigDecimal("0.05"),
                Instant.parse("2026-10-01T00:00:00Z"), null));

        assertThrows(DataIntegrityViolationException.class, () -> ruleVersionPortOut.save(
                version(saved.getPlatformFeeTypeId(), 1, new BigDecimal("0.08"),
                        Instant.parse("2026-11-01T00:00:00Z"), null)));
    }

    @Test
    void shouldRejectOverlappingActivePeriodsAtDatabaseLevel() {
        PlatformFeeType saved = feeTypePortOut.save(feeType("OVERLAPPING_ACTIVE_PERIOD"));
        PlatformFeeRuleVersion first = version(saved.getPlatformFeeTypeId(), 1, new BigDecimal("0.05"),
                Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-12-01T00:00:00Z"));
        first.setStatus(PlatformFeeRuleVersionStatus.ACTIVE);
        ruleVersionPortOut.save(first);

        PlatformFeeRuleVersion overlapping = version(saved.getPlatformFeeTypeId(), 2, new BigDecimal("0.08"),
                Instant.parse("2026-11-01T00:00:00Z"), null);
        overlapping.setStatus(PlatformFeeRuleVersionStatus.ACTIVE);

        assertThrows(DataIntegrityViolationException.class, () -> ruleVersionPortOut.save(overlapping));
    }

    @Test
    void shouldFilterAndPaginateByKeywordAndStatus() {
        feeTypePortOut.save(feeType("PLATFORM_COMMISSION"));
        PlatformFeeType operation = feeType("OPERATION_FEE");
        operation.setStatus(PlatformFeeTypeStatus.ACTIVE);
        feeTypePortOut.save(operation);

        PlatformFeePageResult active = feeTypePortOut.findAll(
                null, PlatformFeeTypeStatus.ACTIVE, null, null, 0, 1);
        assertEquals(1, active.items().size());
        assertEquals(1, active.totalElements());

        PlatformFeePageResult keyword = feeTypePortOut.findAll(
                "commission", null, null, null, 0, 10);
        assertEquals(1, keyword.items().size());

        List<PlatformFeeType> activeByType = feeTypePortOut.findActiveByTransactionType(
                PlatformFeeTransactionType.SUBSCRIPTION_PURCHASE);
        assertEquals(1, activeByType.size());
    }

    private PlatformFeeType feeType(String code) {
        PlatformFeeType type = new PlatformFeeType();
        type.setPlatformFeeTypeId(UUID.randomUUID());
        type.setFeeCode(code);
        type.setName(code);
        type.setTransactionType(PlatformFeeTransactionType.SUBSCRIPTION_PURCHASE);
        type.setCalculationType(PlatformFeeCalculationType.PERCENTAGE);
        type.setFeeBasis(PlatformFeeBasis.GROSS_AMOUNT);
        type.setChargedTo(PlatformFeeChargedTo.PARTNER);
        type.setStackable(true);
        type.setStatus(PlatformFeeTypeStatus.DRAFT);
        return type;
    }

    private PlatformFeeRuleVersion version(UUID feeTypeId, int version, BigDecimal rate, Instant from, Instant to) {
        PlatformFeeRuleVersion ruleVersion = new PlatformFeeRuleVersion();
        ruleVersion.setPlatformFeeRuleVersionId(UUID.randomUUID());
        ruleVersion.setPlatformFeeTypeId(feeTypeId);
        ruleVersion.setVersion(version);
        ruleVersion.setFeeRate(rate);
        ruleVersion.setEffectiveFrom(from);
        ruleVersion.setEffectiveTo(to);
        ruleVersion.setStatus(PlatformFeeRuleVersionStatus.DRAFT);
        return ruleVersion;
    }
}
