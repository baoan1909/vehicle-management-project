package com.ban.vehicle_management.application.billing.platformfee.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.billing.platformfee.port.out.PlatformFeeRuleVersionPortOut;
import com.ban.vehicle_management.application.billing.platformfee.port.out.PlatformFeeTypePortOut;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeResolution;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeRuleVersion;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeBasis;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeCalculationType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeChargedTo;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeRuleVersionStatus;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeScopeType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTypeStatus;
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

@ExtendWith(MockitoExtension.class)
class PlatformFeeResolverUseCaseImplTest {

    @Mock
    private PlatformFeeTypePortOut feeTypePortOut;

    @Mock
    private PlatformFeeRuleVersionPortOut ruleVersionPortOut;

    @InjectMocks
    private PlatformFeeResolverUseCaseImpl resolver;

    private final Instant effectiveAt = Instant.parse("2026-11-15T08:00:00Z");

    @Test
    void shouldReturnEmptyWhenNoActiveFee() {
        when(feeTypePortOut.findActiveByTransactionType(PlatformFeeTransactionType.SUBSCRIPTION_PURCHASE))
                .thenReturn(List.of());

        PlatformFeeResolution resolution = resolver.resolveFees(
                PlatformFeeTransactionType.SUBSCRIPTION_PURCHASE, effectiveAt, 1000000L, 900000L);

        assertTrue(resolution.getFeeLines().isEmpty());
        assertEquals(0L, resolution.getTotalFeeAmount());
        assertEquals(false, resolution.isConfigurationFound());
    }

    @Test
    void shouldFallbackToGlobalWhenSpecificConfigurationIsNotEffective() {
        PlatformFeeType specific = feeType("FUTURE_SPECIFIC", PlatformFeeBasis.GROSS_AMOUNT);
        PlatformFeeType global = feeType("GLOBAL_FEE", PlatformFeeBasis.GROSS_AMOUNT);
        global.setScopeType(PlatformFeeScopeType.GLOBAL);
        global.setTransactionType(null);
        when(feeTypePortOut.findActiveByTransactionType(PlatformFeeTransactionType.PARKING_SESSION))
                .thenReturn(List.of(specific));
        when(ruleVersionPortOut.findEffectiveVersion(specific.getPlatformFeeTypeId(), effectiveAt))
                .thenReturn(Optional.empty());
        when(feeTypePortOut.findActiveByScope(PlatformFeeScopeType.GLOBAL, null))
                .thenReturn(List.of(global));
        when(ruleVersionPortOut.findEffectiveVersion(global.getPlatformFeeTypeId(), effectiveAt))
                .thenReturn(Optional.of(version(global.getPlatformFeeTypeId(), 1, new BigDecimal("0.05"))));

        PlatformFeeResolution resolution = resolver.resolveFees(
                PlatformFeeTransactionType.PARKING_SESSION, effectiveAt, 100000L, 100000L);

        assertEquals(PlatformFeeScopeType.GLOBAL, resolution.getResolvedScope());
        assertTrue(resolution.isFallbackApplied());
        assertEquals(5000L, resolution.getTotalFeeAmount());
    }

    @Test
    void shouldKeepSpecificZeroPercentInsteadOfFallingBackToGlobal() {
        PlatformFeeType exemption = feeType("CARD_DEPOSIT_EXEMPT", PlatformFeeBasis.GROSS_AMOUNT);
        exemption.setTransactionType(PlatformFeeTransactionType.SUBSCRIPTION_CARD_DEPOSIT);
        when(feeTypePortOut.findActiveByTransactionType(
                PlatformFeeTransactionType.SUBSCRIPTION_CARD_DEPOSIT)).thenReturn(List.of(exemption));
        when(ruleVersionPortOut.findEffectiveVersion(exemption.getPlatformFeeTypeId(), effectiveAt))
                .thenReturn(Optional.of(version(exemption.getPlatformFeeTypeId(), 1, BigDecimal.ZERO)));

        PlatformFeeResolution resolution = resolver.resolveFees(
                PlatformFeeTransactionType.SUBSCRIPTION_CARD_DEPOSIT, effectiveAt, 100000L, 100000L);

        assertEquals(PlatformFeeScopeType.TRANSACTION_TYPE, resolution.getResolvedScope());
        assertEquals(false, resolution.isFallbackApplied());
        assertTrue(resolution.isConfigurationFound());
        assertEquals(0L, resolution.getTotalFeeAmount());
    }

    @Test
    void shouldResolveStackedFeesWithDeterministicOrder() {
        PlatformFeeType commission = feeType("PLATFORM_COMMISSION", PlatformFeeBasis.GROSS_AMOUNT);
        PlatformFeeType operation = feeType("OPERATION_FEE", PlatformFeeBasis.GROSS_AMOUNT);
        when(feeTypePortOut.findActiveByTransactionType(PlatformFeeTransactionType.SUBSCRIPTION_PURCHASE))
                .thenReturn(List.of(operation, commission));
        when(ruleVersionPortOut.findEffectiveVersion(eq(commission.getPlatformFeeTypeId()), any()))
                .thenReturn(Optional.of(version(commission.getPlatformFeeTypeId(), 1, new BigDecimal("0.10"))));
        when(ruleVersionPortOut.findEffectiveVersion(eq(operation.getPlatformFeeTypeId()), any()))
                .thenReturn(Optional.of(version(operation.getPlatformFeeTypeId(), 1, new BigDecimal("0.02"))));

        PlatformFeeResolution resolution = resolver.resolveFees(
                PlatformFeeTransactionType.SUBSCRIPTION_PURCHASE, effectiveAt, 1000000L, 900000L);

        assertEquals(2, resolution.getFeeLines().size());
        assertEquals("OPERATION_FEE", resolution.getFeeLines().get(0).getFeeCode());
        assertEquals("PLATFORM_COMMISSION", resolution.getFeeLines().get(1).getFeeCode());
        assertEquals(20000L, resolution.getFeeLines().get(0).getFeeAmount());
        assertEquals(100000L, resolution.getFeeLines().get(1).getFeeAmount());
        assertEquals(120000L, resolution.getTotalFeeAmount());
        assertEquals(new BigDecimal("0.12"), resolution.getEffectiveTotalRate());
    }

    @Test
    void shouldSkipTypeWithoutEffectiveVersion() {
        PlatformFeeType commission = feeType("PLATFORM_COMMISSION", PlatformFeeBasis.GROSS_AMOUNT);
        when(feeTypePortOut.findActiveByTransactionType(PlatformFeeTransactionType.SUBSCRIPTION_PURCHASE))
                .thenReturn(List.of(commission));
        when(ruleVersionPortOut.findEffectiveVersion(eq(commission.getPlatformFeeTypeId()), any()))
                .thenReturn(Optional.empty());

        PlatformFeeResolution resolution = resolver.resolveFees(
                PlatformFeeTransactionType.SUBSCRIPTION_PURCHASE, effectiveAt, 1000000L, 900000L);

        assertTrue(resolution.getFeeLines().isEmpty());
    }

    @Test
    void shouldApplyAfterDiscountBasis() {
        PlatformFeeType commission = feeType("PLATFORM_COMMISSION", PlatformFeeBasis.AFTER_DISCOUNT);
        when(feeTypePortOut.findActiveByTransactionType(PlatformFeeTransactionType.SUBSCRIPTION_PURCHASE))
                .thenReturn(List.of(commission));
        when(ruleVersionPortOut.findEffectiveVersion(eq(commission.getPlatformFeeTypeId()), any()))
                .thenReturn(Optional.of(version(commission.getPlatformFeeTypeId(), 1, new BigDecimal("0.10"))));

        PlatformFeeResolution resolution = resolver.resolveFees(
                PlatformFeeTransactionType.SUBSCRIPTION_PURCHASE, effectiveAt, 1000000L, 900000L);

        assertEquals(90000L, resolution.getFeeLines().get(0).getFeeAmount());
        assertEquals(900000L, resolution.getFeeLines().get(0).getBasisAmount());
    }

    private PlatformFeeType feeType(String code, PlatformFeeBasis basis) {
        PlatformFeeType type = new PlatformFeeType();
        type.setPlatformFeeTypeId(UUID.randomUUID());
        type.setFeeCode(code);
        type.setName(code);
        type.setScopeType(PlatformFeeScopeType.TRANSACTION_TYPE);
        type.setTransactionType(PlatformFeeTransactionType.SUBSCRIPTION_PURCHASE);
        type.setCalculationType(PlatformFeeCalculationType.PERCENTAGE);
        type.setFeeBasis(basis);
        type.setChargedTo(PlatformFeeChargedTo.PARTNER);
        type.setStackable(true);
        type.setStatus(PlatformFeeTypeStatus.ACTIVE);
        return type;
    }

    private PlatformFeeRuleVersion version(UUID feeTypeId, int version, BigDecimal rate) {
        PlatformFeeRuleVersion ruleVersion = new PlatformFeeRuleVersion();
        ruleVersion.setPlatformFeeRuleVersionId(UUID.randomUUID());
        ruleVersion.setPlatformFeeTypeId(feeTypeId);
        ruleVersion.setVersion(version);
        ruleVersion.setFeeRate(rate);
        ruleVersion.setEffectiveFrom(Instant.parse("2026-11-01T00:00:00Z"));
        ruleVersion.setStatus(PlatformFeeRuleVersionStatus.ACTIVE);
        return ruleVersion;
    }
}
