package com.ban.vehicle_management.domain.operations.supportticket.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SupportTicketPolicyTest {

    private final SupportTicketPolicy supportTicketPolicy = new SupportTicketPolicy();

    @Test
    void shouldInitializeSupportTicketWithDefaults() {
        SupportTicket supportTicket = new SupportTicket();
        supportTicket.setCustomerId(UUID.randomUUID());
        supportTicket.setCategoryId(UUID.randomUUID());
        supportTicket.setTitle(" Hoi gia han ");
        supportTicket.setContent(" Noi dung ");

        supportTicketPolicy.initialize(supportTicket);

        assertEquals("Hoi gia han", supportTicket.getTitle());
        assertEquals("Noi dung", supportTicket.getContent());
        assertEquals(SupportTicketStatus.OPEN, supportTicket.getStatus());
        assertEquals(0, supportTicket.getReopenCount());
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
        supportTicketPolicy.resolve(
                supportTicket,
                "Da xu ly",
                Instant.parse("2026-05-15T03:00:00Z")
        );
        supportTicketPolicy.close(
                supportTicket,
                UUID.randomUUID(),
                Instant.parse("2026-05-15T04:00:00Z")
        );

        assertEquals(SupportTicketStatus.CLOSED, supportTicket.getStatus());
    }

    @Test
    void shouldCloseOpenTicketWhenCustomerNoLongerNeedsSupport() {
        SupportTicket supportTicket = validSupportTicket();
        UUID closedBy = UUID.randomUUID();

        supportTicketPolicy.close(
                supportTicket,
                closedBy,
                Instant.parse("2026-05-15T04:00:00Z")
        );

        assertEquals(SupportTicketStatus.CLOSED, supportTicket.getStatus());
        assertEquals(closedBy, supportTicket.getClosedBy());
        assertNotNull(supportTicket.getClosedAt());
        assertNull(supportTicket.getResolvedAt());
        assertNull(supportTicket.getResolutionNote());
    }

    @Test
    void shouldReopenResolvedTicketToOpenWhenTicketHasNoAssignee() {
        SupportTicket supportTicket = resolvedSupportTicket();

        supportTicketPolicy.reopen(supportTicket, Instant.parse("2026-05-16T03:00:00Z"));

        assertEquals(SupportTicketStatus.OPEN, supportTicket.getStatus());
        assertNull(supportTicket.getResolvedAt());
        assertNull(supportTicket.getResolutionNote());
        assertEquals(1, supportTicket.getReopenCount());
    }

    @Test
    void shouldReopenResolvedTicketToInProgressWhenTicketHasAssignee() {
        SupportTicket supportTicket = resolvedSupportTicket();
        supportTicket.setAssignedTo(UUID.randomUUID());

        supportTicketPolicy.reopen(supportTicket, Instant.parse("2026-05-16T03:00:00Z"));

        assertEquals(SupportTicketStatus.IN_PROGRESS, supportTicket.getStatus());
        assertEquals(1, supportTicket.getReopenCount());
    }

    @Test
    void shouldRejectOpenTicketWithResolvedAt() {
        SupportTicket supportTicket = validSupportTicket();
        supportTicket.setResolvedAt(Instant.parse("2026-05-15T03:00:00Z"));

        assertThrows(BadRequestException.class, () -> supportTicketPolicy.validateState(supportTicket));
    }

    @Test
    void shouldRejectTitleExceedingSchemaLength() {
        SupportTicket supportTicket = validSupportTicket();
        supportTicket.setTitle("A".repeat(201));

        assertThrows(BadRequestException.class, () -> supportTicketPolicy.validateState(supportTicket));
    }

    private SupportTicket validSupportTicket() {
        SupportTicket supportTicket = new SupportTicket();
        supportTicket.setSupportTicketId(UUID.randomUUID());
        supportTicket.setCustomerId(UUID.randomUUID());
        supportTicket.setCategoryId(UUID.randomUUID());
        supportTicket.setTitle("Hoi gia han");
        supportTicket.setContent("Noi dung");
        supportTicket.setStatus(SupportTicketStatus.OPEN);
        supportTicket.setReopenCount(0);
        return supportTicket;
    }

    private SupportTicket resolvedSupportTicket() {
        SupportTicket supportTicket = validSupportTicket();
        supportTicket.setStatus(SupportTicketStatus.RESOLVED);
        supportTicket.setResolvedAt(Instant.parse("2026-05-15T03:00:00Z"));
        supportTicket.setResolutionNote("Da xu ly");
        return supportTicket;
    }
}

