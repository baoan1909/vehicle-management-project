package com.ban.vehicle_management.domain.operations.supportticket.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCloseReason;
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
        supportTicket.setRequesterAccountId(UUID.randomUUID());
        supportTicket.setRequesterRoleCode("CUSTOMER");
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
                null,
                Instant.parse("2026-05-15T03:00:00Z")
        );
        supportTicketPolicy.close(
                supportTicket,
                UUID.randomUUID(),
                Instant.parse("2026-05-15T04:00:00Z")
        );

        assertEquals(SupportTicketStatus.CLOSED, supportTicket.getStatus());
        assertNull(supportTicket.getResolutionNote());
        assertEquals(SupportTicketCloseReason.MANUAL, supportTicket.getCloseReason());
        assertNull(supportTicket.getAutoCloseAt());
    }

    @Test
    void shouldScheduleResolvedTicketForAutoCloseAfterTwentyFourHours() {
        SupportTicket supportTicket = validSupportTicket();
        supportTicketPolicy.assign(supportTicket, UUID.randomUUID());
        supportTicketPolicy.startProgress(supportTicket);
        Instant resolvedAt = Instant.parse("2026-05-15T03:00:00Z");

        supportTicketPolicy.resolve(supportTicket, null, resolvedAt);

        assertEquals(resolvedAt.plusSeconds(86_400), supportTicket.getAutoCloseAt());
    }

    @Test
    void shouldExtendOnlyResolvedTicketDeadlineWhenContextActivityOccurs() {
        SupportTicket supportTicket = resolvedSupportTicket();
        Instant originalDeadline = Instant.parse("2026-05-16T03:00:00Z");
        Instant activityAt = Instant.parse("2026-05-15T12:00:00Z");
        supportTicket.setAutoCloseAt(originalDeadline);

        supportTicketPolicy.recordContextActivity(supportTicket, activityAt);

        assertEquals(activityAt, supportTicket.getLastContextActivityAt());
        assertEquals(activityAt.plusSeconds(86_400), supportTicket.getAutoCloseAt());
    }

    @Test
    void shouldAutoCloseResolvedTicketOnlyAfterDeadline() {
        SupportTicket supportTicket = resolvedSupportTicket();
        Instant deadline = Instant.parse("2026-05-16T03:00:00Z");
        supportTicket.setAutoCloseAt(deadline);

        assertThrows(BadRequestException.class, () -> supportTicketPolicy.autoCloseForInactivity(
                supportTicket,
                deadline.minusSeconds(1)
        ));

        supportTicketPolicy.autoCloseForInactivity(supportTicket, deadline);

        assertEquals(SupportTicketStatus.CLOSED, supportTicket.getStatus());
        assertEquals(SupportTicketCloseReason.INACTIVITY, supportTicket.getCloseReason());
        assertEquals(deadline, supportTicket.getClosedAt());
        assertNull(supportTicket.getClosedBy());
        assertNull(supportTicket.getAutoCloseAt());
    }

    @Test
    void shouldRejectClosingOpenTicketBeforeItIsResolved() {
        SupportTicket supportTicket = validSupportTicket();
        UUID closedBy = UUID.randomUUID();

        assertThrows(BadRequestException.class, () -> supportTicketPolicy.close(
                supportTicket,
                closedBy,
                Instant.parse("2026-05-15T04:00:00Z")
        ));
    }

    @Test
    void shouldClaimOnlyUnassignedOpenTicket() {
        SupportTicket supportTicket = validSupportTicket();
        UUID claimant = UUID.randomUUID();

        supportTicketPolicy.claim(supportTicket, claimant);

        assertEquals(claimant, supportTicket.getAssignedTo());
        assertThrows(BadRequestException.class, () -> supportTicketPolicy.claim(supportTicket, UUID.randomUUID()));
    }

    @Test
    void shouldRejectResolvingOpenTicketBeforeItIsAccepted() {
        SupportTicket supportTicket = validSupportTicket();

        assertThrows(BadRequestException.class, () -> supportTicketPolicy.resolve(
                supportTicket,
                null,
                Instant.parse("2026-05-15T04:00:00Z")
        ));
    }

    @Test
    void shouldRejectResolvingTicketThatIsAlreadyResolved() {
        SupportTicket supportTicket = resolvedSupportTicket();
        Instant originalResolvedAt = supportTicket.getResolvedAt();

        assertThrows(BadRequestException.class, () -> supportTicketPolicy.resolve(
                supportTicket,
                null,
                Instant.parse("2026-05-16T04:00:00Z")
        ));

        assertEquals(SupportTicketStatus.RESOLVED, supportTicket.getStatus());
        assertEquals(originalResolvedAt, supportTicket.getResolvedAt());
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
        supportTicket.setAutoCloseAt(Instant.parse("2026-05-16T03:00:00Z"));

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
        supportTicket.setRequesterAccountId(UUID.randomUUID());
        supportTicket.setRequesterRoleCode("CUSTOMER");
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

