package com.ban.vehicle_management.domain.ai.policy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AiToolPolicyTest {

    private final AiToolPolicy policy = new AiToolPolicy();

    @Test
    void shouldAllowOnlyDeclaredTools() {
        assertTrue(policy.isAllowed("getMyTickets"));
        assertTrue(policy.isAllowed("confirmCreateSupportTicket"));
        assertFalse(policy.isAllowed("runSql"));
        assertFalse(policy.isAllowed("httpRequest"));
    }
}
