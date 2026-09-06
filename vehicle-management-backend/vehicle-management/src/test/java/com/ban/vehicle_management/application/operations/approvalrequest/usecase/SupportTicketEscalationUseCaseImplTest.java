package com.ban.vehicle_management.application.operations.approvalrequest.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.application.operations.approvalrequest.model.command.CreateSupportTicketEscalationCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.command.ReviewSupportTicketEscalationCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.SupportTicketEscalationResult;
import com.ban.vehicle_management.application.operations.approvalrequest.port.out.SupportTicketEscalationPortOut;
import com.ban.vehicle_management.application.operations.supportticket.authorization.SupportTicketAccessGuard;
import com.ban.vehicle_management.application.operations.supportticket.port.in.SupportTicketPortIn;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketPortOut;
import com.ban.vehicle_management.application.operations.supportticket.service.SupportTicketConversationService;
import com.ban.vehicle_management.domain.operations.approvalrequest.model.ApprovalRequest;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketEscalationDecision;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketEscalationReason;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class SupportTicketEscalationUseCaseImplTest {

    private static final Instant NOW = Instant.parse("2026-09-06T08:00:00Z");

    @Mock SupportTicketEscalationPortOut escalationPortOut;
    @Mock SupportTicketPortOut supportTicketPortOut;
    @Mock SupportTicketPortIn supportTicketPortIn;
    @Mock SupportTicketAccessGuard accessGuard;
    @Mock SupportTicketConversationService conversationService;
    @Mock NotificationPortIn notificationPortIn;

    private SupportTicketEscalationUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new SupportTicketEscalationUseCaseImpl(
                escalationPortOut,
                supportTicketPortOut,
                supportTicketPortIn,
                accessGuard,
                conversationService,
                notificationPortIn,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void idempotentRetryReturnsOriginalEscalationWithoutCreatingAnotherRow() {
        UUID requester = UUID.randomUUID();
        SupportTicket ticket = activeTicket();
        ApprovalRequest existing = pendingEscalation(ticket, requester);
        when(supportTicketPortOut.findById(ticket.getSupportTicketId())).thenReturn(Optional.of(ticket));
        when(accessGuard.ensureCanCreateOrReadOwnEscalation(ticket)).thenReturn(requester);
        when(escalationPortOut.findByRequesterAndIdempotencyKey(requester, "retry-key"))
                .thenReturn(Optional.of(existing));

        SupportTicketEscalationResult result = useCase.create(
                ticket.getSupportTicketId(),
                new CreateSupportTicketEscalationCommand(
                        SupportTicketEscalationReason.RESPONSE_DELAY, "Nhân viên chưa phản hồi", "retry-key"
                )
        );

        assertEquals(existing.getApprovalRequestId(), result.escalationId());
        verify(escalationPortOut, never()).save(any());
        verify(supportTicketPortIn, never()).assignTicket(any(), any());
    }

    @Test
    void duplicatePendingEscalationIsRejected() {
        UUID requester = UUID.randomUUID();
        SupportTicket ticket = activeTicket();
        when(supportTicketPortOut.findById(ticket.getSupportTicketId())).thenReturn(Optional.of(ticket));
        when(accessGuard.ensureCanCreateOrReadOwnEscalation(ticket)).thenReturn(requester);
        when(escalationPortOut.findPendingByTicketId(ticket.getSupportTicketId()))
                .thenReturn(Optional.of(pendingEscalation(ticket, requester)));

        assertThrows(ConflictException.class, () -> useCase.create(
                ticket.getSupportTicketId(),
                new CreateSupportTicketEscalationCommand(
                        SupportTicketEscalationReason.UNRESOLVED, "Vấn đề chưa được giải quyết", null
                )
        ));
        verify(escalationPortOut, never()).save(any());
    }

    @Test
    void fourthEscalationWithinTwentyFourHoursIsRateLimited() {
        UUID requester = UUID.randomUUID();
        SupportTicket ticket = activeTicket();
        when(supportTicketPortOut.findById(ticket.getSupportTicketId())).thenReturn(Optional.of(ticket));
        when(accessGuard.ensureCanCreateOrReadOwnEscalation(ticket)).thenReturn(requester);
        when(escalationPortOut.findPendingByTicketId(ticket.getSupportTicketId())).thenReturn(Optional.empty());
        when(escalationPortOut.countRecentByRequester(requester, NOW.minusSeconds(24 * 60 * 60)))
                .thenReturn(3L);

        assertThrows(ConflictException.class, () -> useCase.create(
                ticket.getSupportTicketId(),
                new CreateSupportTicketEscalationCommand(
                        SupportTicketEscalationReason.UNRESOLVED,
                        "Vấn đề vẫn chưa được giải quyết thỏa đáng", "rate-limit-key"
                )
        ));
        verify(escalationPortOut, never()).save(any());
    }

    @Test
    void creatingEscalationDoesNotReassignOrInterruptCurrentAssignee() {
        UUID requester = UUID.randomUUID();
        SupportTicket ticket = activeTicket();
        UUID originalAssignee = ticket.getAssignedTo();
        when(supportTicketPortOut.findById(ticket.getSupportTicketId())).thenReturn(Optional.of(ticket));
        when(accessGuard.ensureCanCreateOrReadOwnEscalation(ticket)).thenReturn(requester);
        when(escalationPortOut.findPendingByTicketId(ticket.getSupportTicketId())).thenReturn(Optional.empty());
        when(escalationPortOut.save(any())).thenAnswer(invocation -> {
            ApprovalRequest saved = invocation.getArgument(0);
            saved.setCreatedAt(NOW);
            return saved;
        });

        SupportTicketEscalationResult result = useCase.create(
                ticket.getSupportTicketId(),
                new CreateSupportTicketEscalationCommand(
                        SupportTicketEscalationReason.REQUEST_DIFFERENT_ASSIGNEE,
                        "Tôi muốn một nhân viên khác hỗ trợ", "new-key"
                )
        );

        assertEquals(originalAssignee, result.currentAssigneeId());
        assertEquals(originalAssignee, ticket.getAssignedTo());
        verify(supportTicketPortIn, never()).assignTicket(any(), any());
        verify(conversationService).postAssistantTicketUpdate(any(), any());
    }

    @Test
    void complainedAboutAssigneeCannotReviewOwnEscalation() {
        SupportTicket ticket = activeTicket();
        UUID reviewer = ticket.getAssignedTo();
        ApprovalRequest escalation = pendingEscalation(ticket, UUID.randomUUID());
        when(accessGuard.ensureCanReviewEscalation()).thenReturn(reviewer);
        when(escalationPortOut.findByIdForUpdate(escalation.getApprovalRequestId()))
                .thenReturn(Optional.of(escalation));
        assertThrows(AccessDeniedException.class, () -> useCase.approve(
                escalation.getApprovalRequestId(),
                new ReviewSupportTicketEscalationCommand(
                        SupportTicketEscalationDecision.KEEP_ASSIGNEE, null, "Tiếp tục xử lý"
                )
        ));
        verify(supportTicketPortIn, never()).assignTicket(any(), any());
    }

    @Test
    void approvedReassignmentUsesCanonicalAssignmentFlowAndCompletesRequest() {
        SupportTicket ticket = activeTicket();
        UUID reviewer = UUID.randomUUID();
        UUID newAssignee = UUID.randomUUID();
        ApprovalRequest escalation = pendingEscalation(ticket, UUID.randomUUID());
        when(accessGuard.ensureCanReviewEscalation()).thenReturn(reviewer);
        when(escalationPortOut.findByIdForUpdate(escalation.getApprovalRequestId()))
                .thenReturn(Optional.of(escalation));
        when(supportTicketPortOut.findById(ticket.getSupportTicketId())).thenReturn(Optional.of(ticket));
        when(supportTicketPortIn.assignTicket(ticket.getSupportTicketId(), newAssignee)).thenAnswer(invocation -> {
            ticket.setAssignedTo(newAssignee);
            return ticket;
        });
        when(escalationPortOut.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SupportTicketEscalationResult result = useCase.approve(
                escalation.getApprovalRequestId(),
                new ReviewSupportTicketEscalationCommand(
                        SupportTicketEscalationDecision.REASSIGN, newAssignee, "Chuyển nhân viên phù hợp hơn"
                )
        );

        assertEquals(ApprovalRequestStatus.APPROVED, result.status());
        assertEquals(newAssignee, result.reassignedTo());
        verify(supportTicketPortIn).assignTicket(ticket.getSupportTicketId(), newAssignee);
    }

    @Test
    void secondConcurrentReviewerCannotDecideCompletedEscalation() {
        SupportTicket ticket = activeTicket();
        ApprovalRequest escalation = pendingEscalation(ticket, UUID.randomUUID());
        escalation.setStatus(ApprovalRequestStatus.APPROVED);
        when(accessGuard.ensureCanReviewEscalation()).thenReturn(UUID.randomUUID());
        when(escalationPortOut.findByIdForUpdate(escalation.getApprovalRequestId()))
                .thenReturn(Optional.of(escalation));

        assertThrows(ConflictException.class, () -> useCase.reject(
                escalation.getApprovalRequestId(), "Đã có người xử lý"
        ));
        verify(escalationPortOut, never()).save(any());
    }

    private SupportTicket activeTicket() {
        SupportTicket ticket = new SupportTicket();
        ticket.setSupportTicketId(UUID.randomUUID());
        ticket.setCustomerId(UUID.randomUUID());
        ticket.setAssignedTo(UUID.randomUUID());
        ticket.setStatus(SupportTicketStatus.IN_PROGRESS);
        return ticket;
    }

    private ApprovalRequest pendingEscalation(SupportTicket ticket, UUID requester) {
        ApprovalRequest escalation = new ApprovalRequest();
        escalation.setApprovalRequestId(UUID.randomUUID());
        escalation.setRequestType("SUPPORT_TICKET_ESCALATION");
        escalation.setTargetSchema("operations");
        escalation.setTargetTable("support_tickets");
        escalation.setTargetId(ticket.getSupportTicketId());
        escalation.setStatus(ApprovalRequestStatus.PENDING);
        escalation.setRequestedBy(requester);
        escalation.setCreatedAt(NOW);
        escalation.setRequestData(Map.of(
                "reasonCode", SupportTicketEscalationReason.REQUEST_DIFFERENT_ASSIGNEE.name(),
                "description", "Tôi muốn một nhân viên khác hỗ trợ",
                "currentAssigneeId", ticket.getAssignedTo().toString()
        ));
        return escalation;
    }
}
