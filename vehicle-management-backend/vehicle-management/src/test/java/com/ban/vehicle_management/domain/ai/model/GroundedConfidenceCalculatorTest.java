package com.ban.vehicle_management.domain.ai.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GroundedConfidenceCalculatorTest {

    @Test
    void confidenceShouldBeZeroWithoutActiveIndexOrCitations() {
        GroundedConfidenceCalculator.EvidenceSignals signals = new GroundedConfidenceCalculator.EvidenceSignals(
                0.9, 0.9, 1.0, 1.0, 1.0, false, 3);
        assertEquals(0.0, GroundedConfidenceCalculator.confidence(signals));

        GroundedConfidenceCalculator.EvidenceSignals noCitations = new GroundedConfidenceCalculator.EvidenceSignals(
                0.9, 0.9, 1.0, 1.0, 1.0, true, 0);
        assertEquals(0.0, GroundedConfidenceCalculator.confidence(noCitations));
    }

    @Test
    void confidenceShouldRewardStrongConsistentEvidence() {
        GroundedConfidenceCalculator.EvidenceSignals strong = new GroundedConfidenceCalculator.EvidenceSignals(
                0.95, 0.8, 1.0, 1.0, 1.0, true, 3);
        GroundedConfidenceCalculator.EvidenceSignals weak = new GroundedConfidenceCalculator.EvidenceSignals(
                0.3, 0.1, 0.0, 0.4, 0.5, true, 1);
        double strongConfidence = GroundedConfidenceCalculator.confidence(strong);
        double weakConfidence = GroundedConfidenceCalculator.confidence(weak);
        assertTrue(strongConfidence > weakConfidence);
        assertTrue(strongConfidence >= 0.80);
        assertTrue(weakConfidence < 0.55);
    }

    @Test
    void decideShouldApplyVersionedThresholds() {
        assertEquals(GroundedConfidenceCalculator.EvidenceDecision.GROUNDED,
                GroundedConfidenceCalculator.decide(0.85, 0.80, 0.55));
        assertEquals(GroundedConfidenceCalculator.EvidenceDecision.CAUTIOUS,
                GroundedConfidenceCalculator.decide(0.60, 0.80, 0.55));
        assertEquals(GroundedConfidenceCalculator.EvidenceDecision.INSUFFICIENT,
                GroundedConfidenceCalculator.decide(0.40, 0.80, 0.55));
    }
}
