package com.ban.vehicle_management.domain.ai.model;

/**
 * Computes grounded confidence in [0, 1] from retrieval evidence signals.
 *
 * <p>Inputs are normalized branch signals, never raw RRF scores. Callers must
 * not compare raw RRF or raw cosine values against the 0.80/0.55 business
 * thresholds; only the computed confidence is comparable.</p>
 */
public final class GroundedConfidenceCalculator {

    public static final String POLICY_VERSION = "grounded-confidence-v1";

    private GroundedConfidenceCalculator() {
    }

    public enum EvidenceDecision {
        GROUNDED,
        CAUTIOUS,
        INSUFFICIENT
    }

    public record EvidenceSignals(
            double topVectorScore,
            double topLexicalScore,
            double branchAgreement,
            double citationCoverage,
            double freshness,
            boolean hasActiveIndex,
            int validCitationCount
    ) {
    }

    public static double confidence(EvidenceSignals signals) {
        if (signals == null || !signals.hasActiveIndex() || signals.validCitationCount() <= 0) {
            return 0.0;
        }
        double vector = clamp01(signals.topVectorScore());
        double lexical = clamp01(signals.topLexicalScore());
        double agreement = clamp01(signals.branchAgreement());
        double coverage = clamp01(signals.citationCoverage());
        double freshness = clamp01(signals.freshness());
        double citationBoost = Math.min(1.0, signals.validCitationCount() / 3.0);
        double value = 0.34 * vector
                + 0.16 * lexical
                + 0.20 * agreement
                + 0.16 * coverage
                + 0.08 * freshness
                + 0.06 * citationBoost;
        return clamp01(value);
    }

    public static EvidenceDecision decide(double confidence, double groundedThreshold, double cautionThreshold) {
        if (confidence >= groundedThreshold) {
            return EvidenceDecision.GROUNDED;
        }
        if (confidence >= cautionThreshold) {
            return EvidenceDecision.CAUTIOUS;
        }
        return EvidenceDecision.INSUFFICIENT;
    }

    private static double clamp01(double value) {
        if (Double.isNaN(value)) {
            return 0.0;
        }
        return Math.min(1.0, Math.max(0.0, value));
    }
}
