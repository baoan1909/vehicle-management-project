package com.ban.vehicle_management.application.operations.supportticket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.operations.supportticket.authorization.SupportTicketAccessGuard;
import com.ban.vehicle_management.application.operations.supportticket.model.SupportRequesterContext;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketConversationLinkPortOut;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketPortOut;
import com.ban.vehicle_management.application.operations.supportticket.service.SupportTicketConversationService;
import com.ban.vehicle_management.application.operations.supportticket.usecase.SupportTicketUseCaseImpl;
import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.application.operations.supportticketcategory.port.in.SupportTicketCategoryPortIn;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SupportTicketOwnListLimitTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("00000000-0000-0000-0000-000000000501");

    @Mock
    private SupportTicketPortOut supportTicketPortOut;

    @Mock
    private SupportTicketAccessGuard accessGuard;

    @Mock
    private SupportTicketCategoryPortIn categoryPortIn;

    @Mock
    private NotificationPortIn notificationPortIn;

    @Mock
    private SupportTicketConversationService ticketConversationService;

    @Mock
    private SupportTicketConversationLinkPortOut ticketConversationLinkPortOut;

    @InjectMocks
    private SupportTicketUseCaseImpl useCase;

    @Test
    void shouldDefaultOwnTicketLimitTo100() {
        when(accessGuard.requesterForOwnTickets()).thenReturn(requester());
        when(supportTicketPortOut.findOwnTickets(eq(ACCOUNT_ID), eq(null), eq(null), eq(100)))
                .thenReturn(List.of());

        assertEquals(List.of(), useCase.getMyTickets(null, null, null));
    }

    @Test
    void shouldClampOwnTicketLimit() {
        when(accessGuard.requesterForOwnTickets()).thenReturn(requester());
        when(supportTicketPortOut.findOwnTickets(eq(ACCOUNT_ID), eq(null), eq(null), eq(200)))
                .thenReturn(List.of(new SupportTicket()));

        assertEquals(1, useCase.getMyTickets(null, null, 10000).size());
        verify(supportTicketPortOut).findOwnTickets(eq(ACCOUNT_ID), eq(null), eq(null), eq(200));
    }

    @Test
    void shouldKeepUnboundedLegacyOverloadWorking() {
        when(accessGuard.requesterForOwnTickets()).thenReturn(requester());
        when(supportTicketPortOut.findOwnTickets(eq(ACCOUNT_ID), eq(null), eq(null), eq(100)))
                .thenReturn(List.of());

        assertEquals(List.of(), useCase.getMyTickets(null, null));
    }

    private SupportRequesterContext requester() {
        return new SupportRequesterContext(
                ACCOUNT_ID, "EMPLOYEE", "Requester", "requester@example.com",
                Set.of(), Set.of(), Set.of()
        );
    }
}
