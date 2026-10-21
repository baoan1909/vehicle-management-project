package com.ban.vehicle_management.application.billing.platformfee.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.audit.auditlog.port.out.AuditLogPortOut;
import com.ban.vehicle_management.application.billing.platformfee.authorization.PlatformFeeAccessGuard;
import com.ban.vehicle_management.application.billing.platformfee.config.PlatformFeeProperties;
import com.ban.vehicle_management.application.billing.platformfee.model.result.PlatformFeeConfigurationResult;
import com.ban.vehicle_management.application.billing.platformfee.model.result.PlatformFeePreviewResult;
import com.ban.vehicle_management.application.billing.platformfee.port.in.PlatformFeeResolverPortIn;
import com.ban.vehicle_management.application.billing.platformfee.port.out.PlatformFeeConfigurationPortOut;
import com.ban.vehicle_management.application.billing.platformfee.port.out.PlatformFeeRuleVersionPortOut;
import com.ban.vehicle_management.application.billing.platformfee.port.out.PlatformFeeTypePortOut;
import com.ban.vehicle_management.application.iam.account.port.in.CurrentAccountPortIn;
import com.ban.vehicle_management.domain.audit.auditlog.model.AuditLog;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeResolution;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeRuleVersion;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeBasis;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeCalculationType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeChargedTo;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeRuleVersionStatus;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTypeStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class PlatformFeeManagementUseCaseImplTest {

    @Mock
    private PlatformFeeTypePortOut feeTypePortOut;

    @Mock
    private PlatformFeeRuleVersionPortOut ruleVersionPortOut;

    @Mock
    private PlatformFeeAccessGuard accessGuard;

    @Mock
    private PlatformFeeResolverPortIn resolverPortIn;

    @Mock
    private PlatformFeeProperties properties;

    @Mock
    private PlatformFeeConfigurationPortOut configurationPortOut;

    @Mock
    private AuditLogPortOut auditLogPortOut;

    @Mock
    private CurrentAccountPortIn currentAccountPortIn;

    @InjectMocks
    private PlatformFeeManagementUseCaseImpl useCase;

    @Test
    void shouldReturnConfiguredMaximumPartnerRate() {
        when(configurationPortOut.findGlobal()).thenReturn(Optional.of(configuration("0.275", 3L)));

        assertEquals(new BigDecimal("0.275"), useCase.getMaxPartnerTotalRate());
        verify(accessGuard).ensureCanRead();
    }

    @Test
    void shouldUpdateMaximumPartnerRateWithOptimisticVersion() {
        UUID actorId = UUID.randomUUID();
        PlatformFeeConfigurationResult existing = configuration("0.30", 2L);
        PlatformFeeConfigurationResult updated = new PlatformFeeConfigurationResult(
                new BigDecimal("0.25"), 3L, Instant.parse("2026-10-16T02:00:00Z"), actorId);
        when(configurationPortOut.findGlobal()).thenReturn(Optional.of(existing));
        when(feeTypePortOut.findActiveByTransactionType(PlatformFeeTransactionType.SUBSCRIPTION_PURCHASE))
                .thenReturn(List.of());
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(actorId);
        when(configurationPortOut.updateGlobal(any(), anyLong(), any(), any()))
                .thenReturn(Optional.of(updated));

        PlatformFeeConfigurationResult result = useCase.updateConfiguration(new BigDecimal("0.25"), 2L);

        assertEquals(updated, result);
        verify(accessGuard).ensureCanConfigure();
        verify(configurationPortOut).updateGlobal(eq(new BigDecimal("0.25")), eq(2L), any(), eq(actorId));
        verify(auditLogPortOut).save(any(AuditLog.class));
    }

    @Test
    void shouldRejectStaleConfigurationVersion() {
        when(configurationPortOut.findGlobal()).thenReturn(Optional.of(configuration("0.30", 4L)));

        assertThrows(ConflictException.class,
                () -> useCase.updateConfiguration(new BigDecimal("0.25"), 3L));
    }

    @Test
    void shouldRejectMaximumBelowAnEffectivePartnerFee() {
        PlatformFeeType activeType = draftType();
        activeType.setStatus(PlatformFeeTypeStatus.ACTIVE);
        PlatformFeeRuleVersion activeVersion = draftVersion(activeType.getPlatformFeeTypeId());
        activeVersion.setStatus(PlatformFeeRuleVersionStatus.ACTIVE);
        activeVersion.setFeeRate(new BigDecimal("0.20"));
        activeVersion.setEffectiveFrom(Instant.parse("2020-01-01T00:00:00Z"));

        when(configurationPortOut.findGlobal()).thenReturn(Optional.of(configuration("0.30", 2L)));
        when(feeTypePortOut.findActiveByTransactionType(PlatformFeeTransactionType.SUBSCRIPTION_PURCHASE))
                .thenReturn(List.of(activeType));
        when(ruleVersionPortOut.findActiveByFeeTypeId(activeType.getPlatformFeeTypeId()))
                .thenReturn(List.of(activeVersion));
        when(ruleVersionPortOut.findEffectiveVersion(eq(activeType.getPlatformFeeTypeId()), any()))
                .thenReturn(Optional.of(activeVersion));

        assertThrows(ConflictException.class,
                () -> useCase.updateConfiguration(new BigDecimal("0.15"), 2L));
    }

    @Test
    void shouldPropagateForbiddenWhenGuardDenies() {
        doThrow(new AccessDeniedException("Access is denied")).when(accessGuard).ensureCanCreate();

        assertThrows(AccessDeniedException.class, () -> useCase.createFeeType(draftType()));
    }

    @Test
    void shouldRejectDuplicateFeeCode() {
        when(feeTypePortOut.existsByFeeCode("PLATFORM_COMMISSION")).thenReturn(true);

        assertThrows(ConflictException.class, () -> useCase.createFeeType(draftType()));
    }

    @Test
    void shouldRejectOverlappingVersionActivation() {
        UUID feeTypeId = UUID.randomUUID();
        PlatformFeeType feeType = draftType();
        feeType.setPlatformFeeTypeId(feeTypeId);
        feeType.setStatus(PlatformFeeTypeStatus.ACTIVE);
        PlatformFeeRuleVersion candidate = draftVersion(feeTypeId);
        candidate.setStatus(PlatformFeeRuleVersionStatus.DRAFT);
        PlatformFeeRuleVersion sibling = draftVersion(feeTypeId);
        sibling.setPlatformFeeRuleVersionId(UUID.randomUUID());
        sibling.setStatus(PlatformFeeRuleVersionStatus.ACTIVE);

        when(feeTypePortOut.findById(feeTypeId)).thenReturn(Optional.of(feeType));
        when(ruleVersionPortOut.findById(candidate.getPlatformFeeRuleVersionId()))
                .thenReturn(Optional.of(candidate));
        when(ruleVersionPortOut.findActiveByFeeTypeId(feeTypeId)).thenReturn(List.of(sibling));

        assertThrows(ConflictException.class, () -> useCase.activateRuleVersion(
                feeTypeId, candidate.getPlatformFeeRuleVersionId()));
    }

    @Test
    void shouldRejectActivationWhenTotalRateExceedsLimit() {
        UUID feeTypeId = UUID.randomUUID();
        PlatformFeeType candidate = draftType();
        candidate.setPlatformFeeTypeId(feeTypeId);
        candidate.setStatus(PlatformFeeTypeStatus.DRAFT);
        PlatformFeeRuleVersion candidateVersion = draftVersion(feeTypeId);
        candidateVersion.setStatus(PlatformFeeRuleVersionStatus.DRAFT);
        candidateVersion.setFeeRate(new BigDecimal("0.20"));

        PlatformFeeType other = draftType();
        other.setPlatformFeeTypeId(UUID.randomUUID());
        other.setFeeCode("OPERATION_FEE");
        other.setStatus(PlatformFeeTypeStatus.ACTIVE);
        PlatformFeeRuleVersion otherVersion = draftVersion(other.getPlatformFeeTypeId());
        otherVersion.setFeeRate(new BigDecimal("0.15"));
        otherVersion.setStatus(PlatformFeeRuleVersionStatus.ACTIVE);

        when(feeTypePortOut.findById(feeTypeId)).thenReturn(Optional.of(candidate));
        when(ruleVersionPortOut.findById(candidateVersion.getPlatformFeeRuleVersionId()))
                .thenReturn(Optional.of(candidateVersion));
        when(ruleVersionPortOut.findActiveByFeeTypeId(feeTypeId)).thenReturn(List.of());
        when(feeTypePortOut.findActiveByTransactionType(PlatformFeeTransactionType.SUBSCRIPTION_PURCHASE))
                .thenReturn(List.of(other));
        when(ruleVersionPortOut.findEffectiveVersion(any(), any()))
                .thenReturn(Optional.of(otherVersion));
        when(configurationPortOut.findGlobal()).thenReturn(Optional.of(configuration("0.30", 0L)));

        assertThrows(ConflictException.class, () -> useCase.activateRuleVersion(
                feeTypeId, candidateVersion.getPlatformFeeRuleVersionId()));
    }

    @Test
    void shouldRejectActivationWhenFutureVersionPushesTotalRateAboveLimit() {
        UUID feeTypeId = UUID.randomUUID();
        PlatformFeeType candidate = draftType();
        candidate.setPlatformFeeTypeId(feeTypeId);
        PlatformFeeRuleVersion candidateVersion = draftVersion(feeTypeId);
        candidateVersion.setFeeRate(new BigDecimal("0.20"));

        PlatformFeeType other = draftType();
        other.setPlatformFeeTypeId(UUID.randomUUID());
        other.setFeeCode("FUTURE_OPERATION_FEE");
        other.setStatus(PlatformFeeTypeStatus.ACTIVE);
        PlatformFeeRuleVersion current = draftVersion(other.getPlatformFeeTypeId());
        current.setStatus(PlatformFeeRuleVersionStatus.ACTIVE);
        current.setFeeRate(new BigDecimal("0.05"));
        current.setEffectiveTo(Instant.parse("2026-12-01T00:00:00Z"));
        PlatformFeeRuleVersion future = draftVersion(other.getPlatformFeeTypeId());
        future.setPlatformFeeRuleVersionId(UUID.randomUUID());
        future.setVersion(2);
        future.setStatus(PlatformFeeRuleVersionStatus.ACTIVE);
        future.setFeeRate(new BigDecimal("0.15"));
        future.setEffectiveFrom(Instant.parse("2026-12-01T00:00:00Z"));

        when(feeTypePortOut.findById(feeTypeId)).thenReturn(Optional.of(candidate));
        when(ruleVersionPortOut.findById(candidateVersion.getPlatformFeeRuleVersionId()))
                .thenReturn(Optional.of(candidateVersion));
        when(ruleVersionPortOut.findActiveByFeeTypeId(feeTypeId)).thenReturn(List.of());
        when(feeTypePortOut.findActiveByTransactionType(PlatformFeeTransactionType.SUBSCRIPTION_PURCHASE))
                .thenReturn(List.of(other));
        when(ruleVersionPortOut.findActiveByFeeTypeId(other.getPlatformFeeTypeId()))
                .thenReturn(List.of(current, future));
        when(ruleVersionPortOut.findEffectiveVersion(any(), any())).thenAnswer(invocation -> {
            Instant at = invocation.getArgument(1);
            return at.isBefore(future.getEffectiveFrom()) ? Optional.of(current) : Optional.of(future);
        });
        when(configurationPortOut.findGlobal()).thenReturn(Optional.of(configuration("0.30", 0L)));

        assertThrows(ConflictException.class, () -> useCase.activateRuleVersion(
                feeTypeId, candidateVersion.getPlatformFeeRuleVersionId()));
    }

    @Test
    void shouldRejectFeeTypeActivationWhenAllActiveVersionsExpired() {
        UUID feeTypeId = UUID.randomUUID();
        PlatformFeeType candidate = draftType();
        candidate.setPlatformFeeTypeId(feeTypeId);
        PlatformFeeRuleVersion expired = draftVersion(feeTypeId);
        expired.setStatus(PlatformFeeRuleVersionStatus.ACTIVE);
        expired.setEffectiveFrom(Instant.parse("2020-01-01T00:00:00Z"));
        expired.setEffectiveTo(Instant.parse("2020-02-01T00:00:00Z"));

        when(feeTypePortOut.findById(feeTypeId)).thenReturn(Optional.of(candidate));
        when(ruleVersionPortOut.findActiveByFeeTypeId(feeTypeId)).thenReturn(List.of(expired));

        assertThrows(ConflictException.class, () -> useCase.activateFeeType(feeTypeId));
    }

    @Test
    void shouldCaptureDraftStateBeforeActivatingVersion() {
        UUID feeTypeId = UUID.randomUUID();
        PlatformFeeType feeType = draftType();
        feeType.setPlatformFeeTypeId(feeTypeId);
        PlatformFeeRuleVersion candidate = draftVersion(feeTypeId);

        when(feeTypePortOut.findById(feeTypeId)).thenReturn(Optional.of(feeType));
        when(ruleVersionPortOut.findById(candidate.getPlatformFeeRuleVersionId()))
                .thenReturn(Optional.of(candidate));
        when(ruleVersionPortOut.findActiveByFeeTypeId(feeTypeId)).thenReturn(List.of());
        when(feeTypePortOut.findActiveByTransactionType(PlatformFeeTransactionType.SUBSCRIPTION_PURCHASE))
                .thenReturn(List.of());
        when(configurationPortOut.findGlobal()).thenReturn(Optional.of(configuration("0.30", 0L)));
        when(currentAccountPortIn.getCurrentAccountIdOrThrow()).thenReturn(UUID.randomUUID());
        when(ruleVersionPortOut.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.activateRuleVersion(feeTypeId, candidate.getPlatformFeeRuleVersionId());

        org.mockito.ArgumentCaptor<AuditLog> auditCaptor = org.mockito.ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogPortOut).save(auditCaptor.capture());
        assertEquals("DRAFT", auditCaptor.getValue().getOldData().get("status"));
        assertEquals("ACTIVE", auditCaptor.getValue().getNewData().get("status"));
    }

    @Test
    void shouldPreviewWithLinesAndTotal() {
        PlatformFeeResolution resolution = new PlatformFeeResolution(
                PlatformFeeTransactionType.SUBSCRIPTION_PURCHASE,
                Instant.parse("2026-11-15T08:00:00Z"), 1000000L, 900000L,
                List.of(), 0L, BigDecimal.ZERO);
        when(resolverPortIn.resolveFees(any(), any(), anyLong(), anyLong()))
                .thenReturn(resolution);

        PlatformFeePreviewResult preview = useCase.previewFees(
                PlatformFeeTransactionType.SUBSCRIPTION_PURCHASE,
                Instant.parse("2026-11-15T08:00:00Z"), 1000000L, 900000L);

        assertEquals(0L, preview.totalFeeAmount());
        assertEquals(900000L, preview.partnerRemainingAmount());
    }

    private PlatformFeeType draftType() {
        PlatformFeeType type = new PlatformFeeType();
        type.setPlatformFeeTypeId(UUID.randomUUID());
        type.setFeeCode("PLATFORM_COMMISSION");
        type.setName("Phi nen tang");
        type.setTransactionType(PlatformFeeTransactionType.SUBSCRIPTION_PURCHASE);
        type.setCalculationType(PlatformFeeCalculationType.PERCENTAGE);
        type.setFeeBasis(PlatformFeeBasis.GROSS_AMOUNT);
        type.setChargedTo(PlatformFeeChargedTo.PARTNER);
        type.setStackable(true);
        type.setStatus(PlatformFeeTypeStatus.DRAFT);
        return type;
    }

    private PlatformFeeRuleVersion draftVersion(UUID feeTypeId) {
        PlatformFeeRuleVersion version = new PlatformFeeRuleVersion();
        version.setPlatformFeeRuleVersionId(UUID.randomUUID());
        version.setPlatformFeeTypeId(feeTypeId);
        version.setVersion(1);
        version.setFeeRate(new BigDecimal("0.10"));
        version.setEffectiveFrom(Instant.parse("2026-11-01T00:00:00Z"));
        version.setStatus(PlatformFeeRuleVersionStatus.DRAFT);
        return version;
    }

    private PlatformFeeConfigurationResult configuration(String rate, long version) {
        return new PlatformFeeConfigurationResult(new BigDecimal(rate), version, null, null);
    }
}
