package com.ban.vehicle_management.domain.operations.supportticket.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.shared.enumeration.SupportTicketPriority;
import com.ban.vehicle_management.shared.enumeration.SupportTicketStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SupportTicketPolicyTest {

    private final SupportTicketPolicy supportTicketPolicy = new SupportTicketPolicy();

    @Test
    void shouldInitializeSupportTicketWithDefaults() {
        SupportTicket supportTicket = new SupportTicket();
        supportTicket.setTitle(" Hoi gia han ");
        supportTicket.setContent(" Noi dung ");

        supportTicketPolicy.initialize(supportTicket);

        assertEquals("Hoi gia han", supportTicket.getTitle());
        assertEquals("Noi dung", supportTicket.getContent());
        assertEquals(SupportTicketStatus.OPEN, supportTicket.getStatus());
        assertEquals(SupportTicketPriority.NORMAL, supportTicket.getPriority());
    }

    @Test
    void shouldRequireAssignmentBeforeStartingProgress() {
        SupportTicket supportTicket = validSupportTicket();

        assertThrows(BadRequestException.class, () -> supportTicketPolicy.startProgress(supportTicket));
    }

    @Test
    void shouldMoveTicketThroughSupportFlow() {
        SupportTicket supportTicket = validSupportTicket();
        supportTicketPolicy.assign(supportTicket, UUID.randomUUID());
        supportTicketPolicy.startProgress(supportTicket);
        supportTicketPolicy.resolve(supportTicket, Instant.parse("2026-05-15T03:00:00Z"));
        supportTicketPolicy.close(supportTicket);

        assertEquals(SupportTicketStatus.CLOSED, supportTicket.getStatus());
    }

    @Test
    void shouldRejectOpenTicketWithResolvedAt() {
        SupportTicket supportTicket = validSupportTicket();
        supportTicket.setResolvedAt(Instant.parse("2026-05-15T03:00:00Z"));

        assertThrows(BadRequestException.class, () -> supportTicketPolicy.validateState(supportTicket));
    }

    private SupportTicket validSupportTicket() {
        SupportTicket supportTicket = new SupportTicket();
        supportTicket.setSupportTicketId(UUID.randomUUID());
        supportTicket.setTitle("Hoi gia han");
        supportTicket.setContent("Noi dung");
        supportTicket.setStatus(SupportTicketStatus.OPEN);
        supportTicket.setPriority(SupportTicketPriority.NORMAL);
        return supportTicket;
    }
}

