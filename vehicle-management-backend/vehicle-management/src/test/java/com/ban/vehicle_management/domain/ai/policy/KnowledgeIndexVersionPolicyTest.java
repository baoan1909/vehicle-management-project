package com.ban.vehicle_management.domain.ai.policy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ban.vehicle_management.shared.enumeration.ai.KnowledgeIndexVersionStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;

class KnowledgeIndexVersionPolicyTest {

    @Test
    void shouldAllowDocumentedTransitions() {
        assertTrue(KnowledgeIndexVersionPolicy.canTransition(KnowledgeIndexVersionStatus.DRAFT, KnowledgeIndexVersionStatus.BUILDING));
        assertTrue(KnowledgeIndexVersionPolicy.canTransition(KnowledgeIndexVersionStatus.BUILDING, KnowledgeIndexVersionStatus.READY));
        assertTrue(KnowledgeIndexVersionPolicy.canTransition(KnowledgeIndexVersionStatus.BUILDING, KnowledgeIndexVersionStatus.FAILED));
        assertTrue(KnowledgeIndexVersionPolicy.canTransition(KnowledgeIndexVersionStatus.READY, KnowledgeIndexVersionStatus.ACTIVE));
        assertTrue(KnowledgeIndexVersionPolicy.canTransition(KnowledgeIndexVersionStatus.ACTIVE, KnowledgeIndexVersionStatus.RETIRED));
        assertTrue(KnowledgeIndexVersionPolicy.canTransition(KnowledgeIndexVersionStatus.RETIRED, KnowledgeIndexVersionStatus.ACTIVE));
    }

    @Test
    void shouldRejectInvalidTransitions() {
        assertFalse(KnowledgeIndexVersionPolicy.canTransition(KnowledgeIndexVersionStatus.DRAFT, KnowledgeIndexVersionStatus.ACTIVE));
        assertFalse(KnowledgeIndexVersionPolicy.canTransition(KnowledgeIndexVersionStatus.FAILED, KnowledgeIndexVersionStatus.ACTIVE));
        assertFalse(KnowledgeIndexVersionPolicy.canTransition(KnowledgeIndexVersionStatus.ACTIVE, KnowledgeIndexVersionStatus.BUILDING));
        assertFalse(KnowledgeIndexVersionPolicy.canTransition(KnowledgeIndexVersionStatus.READY, KnowledgeIndexVersionStatus.READY));
        assertFalse(KnowledgeIndexVersionPolicy.canTransition(null, KnowledgeIndexVersionStatus.READY));
    }

    @Test
    void assertTransitionShouldThrowForInvalidMove() {
        assertThrows(
                BadRequestException.class,
                () -> KnowledgeIndexVersionPolicy.assertTransition(
                        KnowledgeIndexVersionStatus.DRAFT,
                        KnowledgeIndexVersionStatus.ACTIVE
                )
        );
    }
}