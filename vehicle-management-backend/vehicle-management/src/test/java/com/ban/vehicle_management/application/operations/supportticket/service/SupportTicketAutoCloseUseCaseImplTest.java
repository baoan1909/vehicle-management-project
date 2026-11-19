package com.ban.vehicle_management.application.operations.supportticket.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketPortOut;
import com.ban.vehicle_management.application.operations.supportticket.service.SupportTicketConversationService;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCloseReason;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketEventType;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SupportTicketAutoCloseUseCaseImplTest {

    private static final Instant NOW = Instant.parse("2026-11-18T03:00:00Z");

    @Mock
    private SupportTicketPortOut supportTicketPortOut;
    @Mock
    private SupportTicketConversationService ticketConversationService;
    @Mock
    private NotificationPortIn notificationPortIn;

    private SupportTicketAutoCloseUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new SupportTicketAutoCloseUseCaseImpl(
                supportTicketPortOut,
                ticketConversationService,
                notificationPortIn,
                Clock.fixed(NOW, ZoneOffset.UTC),
                25
        );
    }

    @Test
    void closesEachDueResolvedTicketAndPublishesLifecycleSnapshot() {
        SupportTicket ticket = dueResolvedTicket();
        when(supportTicketPortOut.findDueForAutoClose(NOW, 25)).thenReturn(List.of(ticket));
        when(supportTicketPortOut.save(ticket)).thenReturn(ticket);

        int closed = useCase.closeDueTickets();

        assertEquals(1, closed);
        assertEquals(SupportTicketStatus.CLOSED, ticket.getStatus());
        assertEquals(SupportTicketCloseReason.INACTIVITY, ticket.getCloseReason());
        assertEquals(NOW, ticket.getClosedAt());
        assertNull(ticket.getClosedBy());
        assertNull(ticket.getAutoCloseAt());
        verify(ticketConversationService).postSystemTicketStatusCard(
                eq(ticket),
                eq(SupportTicketEventType.CLOSED),
                eq(SupportTicketStatus.RESOLVED),
                eq("auto-closed:" + ticket.getSupportTicketId() + ":0")
        );
        verify(ticketConversationService).postAssistantTicketUpdate(eq(ticket), any(String.class));
        verify(notificationPortIn).sendWebNotification(any());
    }

    @Test
    void doesNothingWhenNoTicketHasReachedItsOwnDeadline() {
        when(supportTicketPortOut.findDueForAutoClose(NOW, 25)).thenReturn(List.of());

        assertEquals(0, useCase.closeDueTickets());

        verify(supportTicketPortOut, never()).save(any());
        verify(ticketConversationService, never()).postSystemTicketStatusCard(any(), any(), any(), any());
        verify(notificationPortIn, never()).sendWebNotification(any());
    }

    private SupportTicket dueResolvedTicket() {
        SupportTicket ticket = new SupportTicket();
        ticket.setSupportTicketId(UUID.randomUUID());
        ticket.setRequesterAccountId(UUID.randomUUID());
        ticket.setRequesterRoleCode("CUSTOMER");
        ticket.setCategoryId(UUID.randomUUID());
        ticket.setTitle("Cần hỗ trợ vận hành");
        ticket.setContent("Nội dung yêu cầu hỗ trợ");
        ticket.setAssignedTo(UUID.randomUUID());
        ticket.setStatus(SupportTicketStatus.RESOLVED);
        ticket.setResolvedAt(NOW.minusSeconds(86_400));
        ticket.setAutoCloseAt(NOW);
        ticket.setReopenCount(0);
        return ticket;
    }
}
