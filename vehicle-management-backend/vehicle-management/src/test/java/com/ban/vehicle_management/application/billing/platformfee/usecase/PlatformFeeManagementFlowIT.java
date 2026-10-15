package com.ban.vehicle_management.application.billing.platformfee.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;

import com.ban.vehicle_management.application.billing.platformfee.model.result.PlatformFeePreviewResult;
import com.ban.vehicle_management.application.billing.platformfee.port.in.PlatformFeeManagementPortIn;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeRuleVersion;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeBasis;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeCalculationType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeChargedTo;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PlatformFeeManagementFlowIT extends com.ban.vehicle_management.testsupport.PostgresIntegrationTestSupport {

    @Autowired
    private PlatformFeeManagementPortIn managementPortIn;

    @MockitoBean
    private CurrentAccountPortIn currentAccountPortIn;

    @MockitoBean
    private AuditLogPortOut auditLogPortOut;

    @Test
    void shouldRunFullLifecycleFromDraftToPreview() {
        UUID actorId = UUID.randomUUID();
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(actorId);

        PlatformFeeType commission = newType("PLATFORM_COMMISSION_FLOW", PlatformFeeBasis.GROSS_AMOUNT);
        PlatformFeeType savedCommission = managementPortIn.createFeeType(commission);

        PlatformFeeRuleVersion v1 = managementPortIn.createRuleVersion(
                savedCommission.getPlatformFeeTypeId(), new BigDecimal("0.10"),
                Instant.parse("2026-11-01T00:00:00Z"), null);
        assertEquals(1, v1.getVersion());

        managementPortIn.activateRuleVersion(savedCommission.getPlatformFeeTypeId(),
                v1.getPlatformFeeRuleVersionId());
        managementPortIn.activateFeeType(savedCommission.getPlatformFeeTypeId());

        PlatformFeeType operation = newType("OPERATION_FEE_FLOW", PlatformFeeBasis.GROSS_AMOUNT);
        PlatformFeeType savedOperation = managementPortIn.createFeeType(operation);
        PlatformFeeRuleVersion operationV1 = managementPortIn.createRuleVersion(
                savedOperation.getPlatformFeeTypeId(), new BigDecimal("0.02"),
                Instant.parse("2026-11-01T00:00:00Z"), null);
        managementPortIn.activateRuleVersion(savedOperation.getPlatformFeeTypeId(),
                operationV1.getPlatformFeeRuleVersionId());
        managementPortIn.activateFeeType(savedOperation.getPlatformFeeTypeId());

        PlatformFeePreviewResult preview = managementPortIn.previewFees(
                PlatformFeeTransactionType.SUBSCRIPTION_PURCHASE,
                Instant.parse("2026-11-15T08:00:00Z"), 1000000L, 900000L);

        assertEquals(2, preview.feeLines().size());
        assertEquals(120000L, preview.totalFeeAmount());
        assertEquals(new BigDecimal("0.12"), preview.effectiveTotalRate());

        PlatformFeeRuleVersion v2 = managementPortIn.createRuleVersion(
                savedCommission.getPlatformFeeTypeId(), new BigDecimal("0.12"),
                Instant.parse("2026-12-01T00:00:00Z"), null);
        assertEquals(2, v2.getVersion());

        managementPortIn.deactivateFeeType(savedOperation.getPlatformFeeTypeId());
        managementPortIn.archiveFeeType(savedOperation.getPlatformFeeTypeId());

        assertThrows(ConflictException.class, () -> managementPortIn.updateDraftFeeType(
                savedCommission.getPlatformFeeTypeId(), savedCommission));

        org.mockito.ArgumentCaptor<com.ban.vehicle_management.domain.audit.auditlog.model.AuditLog> auditCaptor =
                org.mockito.ArgumentCaptor.forClass(
                        com.ban.vehicle_management.domain.audit.auditlog.model.AuditLog.class);
        verify(auditLogPortOut, atLeastOnce()).save(auditCaptor.capture());
        java.util.List<String> actions = auditCaptor.getAllValues().stream()
                .map(com.ban.vehicle_management.domain.audit.auditlog.model.AuditLog::getAction)
                .toList();
        for (String expected : java.util.List.of("CREATE_FEE_TYPE", "CREATE_FEE_RULE_VERSION",
                "ACTIVATE_FEE_RULE_VERSION", "ACTIVATE_FEE_TYPE", "DEACTIVATE_FEE_TYPE", "ARCHIVE_FEE_TYPE")) {
            org.junit.jupiter.api.Assertions.assertTrue(actions.contains(expected),
                    "Missing audit action " + expected);
        }
    }

    @Test
    void shouldRejectOverlappingVersionActivationEndToEnd() {
        UUID actorId = UUID.randomUUID();
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(actorId);

        PlatformFeeType saved = managementPortIn.createFeeType(newType("OVERLAP_FEE_FLOW", PlatformFeeBasis.GROSS_AMOUNT));
        PlatformFeeRuleVersion v1 = managementPortIn.createRuleVersion(
                saved.getPlatformFeeTypeId(), new BigDecimal("0.05"),
                Instant.parse("2026-11-01T00:00:00Z"), null);
        managementPortIn.activateRuleVersion(saved.getPlatformFeeTypeId(), v1.getPlatformFeeRuleVersionId());

        PlatformFeeRuleVersion v2 = managementPortIn.createRuleVersion(
                saved.getPlatformFeeTypeId(), new BigDecimal("0.08"),
                Instant.parse("2026-11-15T00:00:00Z"), null);

        assertThrows(ConflictException.class, () -> managementPortIn.activateRuleVersion(
                saved.getPlatformFeeTypeId(), v2.getPlatformFeeRuleVersionId()));
    }

    private PlatformFeeType newType(String code, PlatformFeeBasis basis) {
        PlatformFeeType type = new PlatformFeeType();
        type.setPlatformFeeTypeId(UUID.randomUUID());
        type.setFeeCode(code);
        type.setName(code);
        type.setTransactionType(PlatformFeeTransactionType.SUBSCRIPTION_PURCHASE);
        type.setCalculationType(PlatformFeeCalculationType.PERCENTAGE);
        type.setFeeBasis(basis);
        type.setChargedTo(PlatformFeeChargedTo.PARTNER);
        type.setStackable(true);
        return type;
    }
}
