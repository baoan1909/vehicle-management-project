package com.ban.vehicle_management.application.billing.platformfee.usecase;

import com.ban.vehicle_management.application.billing.platformfee.port.in.PlatformFeeResolverPortIn;
import com.ban.vehicle_management.application.billing.platformfee.port.out.PlatformFeeRuleVersionPortOut;
import com.ban.vehicle_management.application.billing.platformfee.port.out.PlatformFeeTypePortOut;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeLine;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeResolution;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeRuleVersion;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeType;
import com.ban.vehicle_management.domain.billing.platformfee.policy.PlatformFeeCalculationPolicy;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeTransactionType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeScopeType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformFeeResolverUseCaseImpl implements PlatformFeeResolverPortIn {

    private final PlatformFeeTypePortOut feeTypePortOut;
    private final PlatformFeeRuleVersionPortOut ruleVersionPortOut;
    private final PlatformFeeCalculationPolicy calculationPolicy = new PlatformFeeCalculationPolicy();

    public PlatformFeeResolverUseCaseImpl(
            PlatformFeeTypePortOut feeTypePortOut,
            PlatformFeeRuleVersionPortOut ruleVersionPortOut) {
        this.feeTypePortOut = feeTypePortOut;
        this.ruleVersionPortOut = ruleVersionPortOut;
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformFeeResolution resolveFees(PlatformFeeTransactionType transactionType,
            Instant effectiveAt, long grossAmount, long customerPaidAmount) {
        if (transactionType == null) {
            throw new BadRequestException("transactionType must not be null");
        }
        Instant resolvedAt = effectiveAt == null ? Instant.now() : effectiveAt;
        if (grossAmount < 0 || customerPaidAmount < 0) {
            throw new BadRequestException("Amounts must not be negative");
        }

        List<PlatformFeeType> activeTypes = feeTypePortOut.findActiveByTransactionType(transactionType)
                .stream()
                .sorted(Comparator.comparing(PlatformFeeType::getFeeCode,
                        Comparator.nullsLast(String::compareTo)))
                .toList();

        List<PlatformFeeLine> lines = resolveLines(activeTypes, resolvedAt, grossAmount, customerPaidAmount);
        PlatformFeeScopeType resolvedScope = PlatformFeeScopeType.TRANSACTION_TYPE;
        boolean fallbackApplied = false;
        if (lines.isEmpty()) {
            activeTypes = feeTypePortOut.findActiveByScope(PlatformFeeScopeType.GLOBAL, null)
                    .stream()
                    .sorted(Comparator.comparing(PlatformFeeType::getFeeCode,
                            Comparator.nullsLast(String::compareTo)))
                    .toList();
            lines = resolveLines(activeTypes, resolvedAt, grossAmount, customerPaidAmount);
            resolvedScope = lines.isEmpty() ? null : PlatformFeeScopeType.GLOBAL;
            fallbackApplied = !lines.isEmpty();
        }

        BigDecimal totalRate = BigDecimal.ZERO;
        for (PlatformFeeLine line : lines) {
            if (line.getFeeRate() != null) {
                totalRate = totalRate.add(line.getFeeRate());
            }
        }

        lines.sort(Comparator.comparing(PlatformFeeLine::getFeeCode, Comparator.nullsLast(String::compareTo)));
        long totalFeeAmount = lines.stream().mapToLong(PlatformFeeLine::getFeeAmount).sum();

        return new PlatformFeeResolution(
                transactionType, resolvedScope, fallbackApplied, !lines.isEmpty(), resolvedAt,
                grossAmount, customerPaidAmount, List.copyOf(lines), totalFeeAmount, totalRate);
    }

    private List<PlatformFeeLine> resolveLines(List<PlatformFeeType> activeTypes, Instant effectiveAt,
            long grossAmount, long customerPaidAmount) {
        List<PlatformFeeLine> lines = new ArrayList<>();
        for (PlatformFeeType feeType : activeTypes) {
            PlatformFeeRuleVersion effectiveVersion =
                    ruleVersionPortOut.findEffectiveVersion(feeType.getPlatformFeeTypeId(), effectiveAt)
                            .orElse(null);
            if (effectiveVersion == null) {
                continue;
            }
            lines.add(calculationPolicy.toFeeLine(feeType, effectiveVersion, grossAmount, customerPaidAmount));
        }
        return lines;
    }
}
