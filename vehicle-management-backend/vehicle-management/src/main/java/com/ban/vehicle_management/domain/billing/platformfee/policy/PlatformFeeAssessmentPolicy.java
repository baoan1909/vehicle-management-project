package com.ban.vehicle_management.domain.billing.platformfee.policy;

import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeAssessment;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeAssessmentLine;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeLine;
import com.ban.vehicle_management.domain.billing.platformfee.model.PlatformFeeResolution;
import com.ban.vehicle_management.shared.enumeration.billing.FundCustody;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeAssessmentStatus;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeCalculationType;
import com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeChargedTo;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

public class PlatformFeeAssessmentPolicy {

    public PlatformFeeAssessment createApplied(UUID invoiceId, UUID paymentId, UUID organizationId,
            FundCustody custody, PlatformFeeResolution resolution, Instant appliedAt) {
        if (resolution == null || !resolution.isConfigurationFound()
                || resolution.getResolvedScope() == null) {
            throw new ConflictException("No effective platform fee configuration was found for the transaction");
        }
        Instant resolvedAt = appliedAt == null ? Instant.now() : appliedAt;
        UUID assessmentId = UUID.randomUUID();
        PlatformFeeAssessment assessment = new PlatformFeeAssessment();
        assessment.setPlatformFeeAssessmentId(assessmentId);
        assessment.setInvoiceId(invoiceId);
        assessment.setPaymentId(paymentId);
        assessment.setOrganizationId(organizationId);
        assessment.setRequestedTransactionType(resolution.getRequestedTransactionType());
        assessment.setResolvedScope(resolution.getResolvedScope());
        assessment.setFallbackApplied(resolution.isFallbackApplied());
        assessment.setBasisAmount(vnd(resolution.getCustomerPaidAmount()));
        long partnerFeeAmount = resolution.getFeeLines().stream()
                .filter(line -> PlatformFeeChargedTo.PARTNER.equals(line.getChargedTo()))
                .mapToLong(PlatformFeeLine::getFeeAmount)
                .sum();
        assessment.setTotalFeeAmount(vnd(partnerFeeAmount));
        assessment.setCurrency("VND");
        assessment.setFundCustody(custody);
        assessment.setCollector(custody);
        assessment.setAmountFingerprint(fingerprint(resolution));
        assessment.setStatus(PlatformFeeAssessmentStatus.APPLIED);
        assessment.setAppliedAt(resolvedAt);
        assessment.setCreatedAt(resolvedAt);
        assessment.setLines(toLines(assessmentId, resolution, resolvedAt));
        return assessment;
    }

    private List<PlatformFeeAssessmentLine> toLines(UUID assessmentId,
            PlatformFeeResolution resolution, Instant createdAt) {
        return resolution.getFeeLines().stream()
                .map(line -> toLine(assessmentId, resolution, line, createdAt))
                .toList();
    }

    private PlatformFeeAssessmentLine toLine(UUID assessmentId, PlatformFeeResolution resolution,
            PlatformFeeLine line, Instant createdAt) {
        PlatformFeeAssessmentLine snapshot = new PlatformFeeAssessmentLine();
        snapshot.setPlatformFeeAssessmentLineId(UUID.randomUUID());
        snapshot.setPlatformFeeAssessmentId(assessmentId);
        snapshot.setFeeTypeId(line.getFeeTypeId());
        snapshot.setFeeRuleVersionId(line.getRuleVersionId());
        snapshot.setFeeCode(line.getFeeCode());
        snapshot.setFeeName(line.getFeeName());
        snapshot.setScopeType(resolution.getResolvedScope());
        snapshot.setTransactionType(resolution.getResolvedScope()
                == com.ban.vehicle_management.shared.enumeration.billing.PlatformFeeScopeType.GLOBAL
                ? null : resolution.getRequestedTransactionType());
        snapshot.setCalculationType(line.getCalculationType() == null
                ? PlatformFeeCalculationType.PERCENTAGE : line.getCalculationType());
        snapshot.setFeeBasis(line.getFeeBasis());
        snapshot.setChargedTo(line.getChargedTo());
        snapshot.setFeeRate(line.getFeeRate());
        snapshot.setBasisAmount(vnd(line.getBasisAmount()));
        snapshot.setCalculatedFeeAmount(vnd(line.getFeeAmount()));
        snapshot.setFinalFeeAmount(vnd(line.getFeeAmount()));
        snapshot.setStackable(line.isStackable());
        snapshot.setRuleVersion(line.getVersion());
        snapshot.setCreatedAt(createdAt);
        return snapshot;
    }

    private String fingerprint(PlatformFeeResolution resolution) {
        String source = resolution.getRequestedTransactionType() + "|"
                + resolution.getGrossAmount() + "|" + resolution.getCustomerPaidAmount() + "|"
                + resolution.getTotalFeeAmount() + "|" + resolution.getFeeLines().stream()
                .map(line -> line.getRuleVersionId() + ":" + line.getFeeAmount())
                .sorted().reduce("", (left, right) -> left + "|" + right);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private BigDecimal vnd(long value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.UNNECESSARY);
    }
}
