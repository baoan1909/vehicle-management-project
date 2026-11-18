package com.ban.vehicle_management.application.operations.supportticket.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ban.vehicle_management.application.notification.notification.port.in.NotificationPortIn;
import com.ban.vehicle_management.application.operations.supportticket.authorization.SupportTicketAccessGuard;
import com.ban.vehicle_management.application.operations.supportticket.model.SupportRequesterContext;
import com.ban.vehicle_management.application.operations.supportticket.model.SupportTicketChatIntake;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketConversationLinkPortOut;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketPortOut;
import com.ban.vehicle_management.application.operations.supportticket.usecase.SupportTicketUseCaseImpl;
import com.ban.vehicle_management.application.operations.supportticketcategory.port.in.SupportTicketCategoryPortIn;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.domain.operations.supportticketcategory.model.SupportTicketCategory;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationType;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketSource;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketEventType;
import com.ban.vehicle_management.shared.exception.ConflictException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class SupportTicketUseCaseImplTest {

    @Mock SupportTicketPortOut supportTicketPortOut;
    @Mock SupportTicketAccessGuard accessGuard;
    @Mock SupportTicketCategoryPortIn categoryPortIn;
    @Mock NotificationPortIn notificationPortIn;
    @Mock SupportTicketConversationService ticketConversationService;
    @Mock SupportTicketConversationLinkPortOut ticketConversationLinkPortOut;
    @InjectMocks SupportTicketUseCaseImpl useCase;

    @Test
    void chatIntakeRetryReusesRequesterTicketAndRepairsMissingCardIdempotently() {
        UUID accountId = UUID.randomUUID();
        SupportRequesterContext requester = requester(accountId);
        ChatConversation assistant = assistantConversation(accountId);
        SupportTicket existing = ticket(accountId);
        when(accessGuard.requesterForCreate()).thenReturn(requester);
        when(ticketConversationService.openOrCreateAssistantConversation(accountId)).thenReturn(assistant);
        when(supportTicketPortOut.findByRequesterAccountIdAndIdempotencyKey(accountId, "request-1"))
                .thenReturn(Optional.of(existing));

        SupportTicketChatIntake result = useCase.createChatIntake(new SupportTicket(), " request-1 ");

        assertSame(existing, result.ticket());
        assertSame(assistant, result.conversation());
        assertTrue(result.reusedActiveTicket());
        verify(supportTicketPortOut).lockRequesterSupport(accountId);
        verify(ticketConversationService).postTicketCardFromRequester(existing, assistant, accountId);
        verify(supportTicketPortOut, never()).save(any());
    }

    @Test
    void newChatIntakePersistsRequesterSourceAndIdempotencyKey() {
        UUID accountId = UUID.randomUUID();
        SupportRequesterContext requester = requester(accountId);
        ChatConversation assistant = assistantConversation(accountId);
        SupportTicket request = ticket(null);
        SupportTicketCategory category = category(request.getCategoryId());
        when(accessGuard.requesterForCreate()).thenReturn(requester);
        when(ticketConversationService.openOrCreateAssistantConversation(accountId)).thenReturn(assistant);
        when(supportTicketPortOut.findByRequesterAccountIdAndIdempotencyKey(accountId, "request-2"))
                .thenReturn(Optional.empty());
        when(categoryPortIn.ensureAvailableCategory(request.getCategoryId())).thenReturn(category);
        when(supportTicketPortOut.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SupportTicketChatIntake result = useCase.createChatIntake(request, "request-2");

        assertFalse(result.reusedActiveTicket());
        assertEquals(accountId, result.ticket().getRequesterAccountId());
        assertEquals("CUSTOMER", result.ticket().getRequesterRoleCode());
        assertEquals(SupportTicketSource.ASSISTANT_CHAT, result.ticket().getSource());
        assertEquals(assistant.getConversationId(), result.ticket().getSourceConversationId());
        assertEquals("request-2", result.ticket().getIdempotencyKey());
        assertEquals(SupportTicketStatus.OPEN, result.ticket().getStatus());
        verify(ticketConversationService).postTicketCardFromRequester(result.ticket(), assistant, accountId);
        verify(notificationPortIn).sendBroadcastWebNotification(argThat(command ->
                command.notificationType().name().equals("SUPPORT_TICKET_CREATED")
                        && command.redirectUrl().equals("/admin/support-tickets?ticketId=" + result.ticket().getSupportTicketId())
                        && command.recipientCriteria() != null
                        && !command.recipientCriteria().requireBusinessAccess()
                        && !command.recipientCriteria().allowNoRecipients()
                        && command.recipientCriteria().requiredAnyPermissionCodes().contains("SUPPORT_TICKET_PROCESS_ALL")
                        && command.recipientCriteria().excludedAccountIds().contains(accountId)
        ));
    }

    @Test
    void myTicketsAlwaysUsesAuthenticatedRequesterAccount() {
        UUID accountId = UUID.randomUUID();
        SupportTicket owned = ticket(accountId);
        when(accessGuard.requesterForOwnTickets()).thenReturn(requester(accountId));
        when(supportTicketPortOut.findOwnTickets(accountId, SupportTicketStatus.OPEN, "gate", 100))
                .thenReturn(List.of(owned));

        assertEquals(List.of(owned), useCase.getMyTickets(SupportTicketStatus.OPEN, " gate "));
    }

    @Test
    void requesterCannotShareAnotherAccountsTicketIntoAssistantConversation() {
        UUID currentAccountId = UUID.randomUUID();
        SupportTicket foreignTicket = ticket(UUID.randomUUID());
        when(accessGuard.requesterForAssistant()).thenReturn(requester(currentAccountId));
        when(supportTicketPortOut.findById(foreignTicket.getSupportTicketId())).thenReturn(Optional.of(foreignTicket));

        assertThrows(AccessDeniedException.class,
                () -> useCase.shareTicketWithAssistant(foreignTicket.getSupportTicketId(), "assistant-share-denied"));

        verify(ticketConversationService, never()).openOrCreateAssistantConversation(any());
        verify(ticketConversationService, never()).postTicketCardFromRequester(any(), any(), any());
    }

    @Test
    void sharingOwnedTicketOnlyAddsReferenceCardAndPreservesLifecycle() {
        UUID accountId = UUID.randomUUID();
        UUID assignee = UUID.randomUUID();
        SupportTicket ownedTicket = ticket(accountId);
        ownedTicket.setAssignedTo(assignee);
        ownedTicket.setStatus(SupportTicketStatus.IN_PROGRESS);
        ChatConversation assistant = assistantConversation(accountId);
        when(accessGuard.requesterForAssistant()).thenReturn(requester(accountId));
        when(supportTicketPortOut.findById(ownedTicket.getSupportTicketId())).thenReturn(Optional.of(ownedTicket));
        when(ticketConversationService.openOrCreateAssistantConversation(accountId)).thenReturn(assistant);

        SupportTicket result = useCase.shareTicketWithAssistant(
                ownedTicket.getSupportTicketId(), "assistant-share-1"
        );

        assertSame(ownedTicket, result);
        assertEquals(assignee, result.getAssignedTo());
        assertEquals(SupportTicketStatus.IN_PROGRESS, result.getStatus());
        verify(ticketConversationService).repostTicketCardFromRequester(
                ownedTicket, assistant, accountId, "assistant-share-1"
        );
        verify(supportTicketPortOut, never()).save(any());
    }

    @Test
    void assignedHandlerSharingOpenTicketMarksFirstResponseAndResurfacesCard() {
        UUID requesterAccountId = UUID.randomUUID();
        UUID handlerAccountId = UUID.randomUUID();
        SupportTicket openTicket = ticket(requesterAccountId);
        openTicket.setAssignedTo(handlerAccountId);
        ChatConversation direct = assistantConversation(requesterAccountId);
        direct.setConversationType(ChatConversationType.SUPPORT_REQUESTER_DIRECT);
        when(supportTicketPortOut.findById(openTicket.getSupportTicketId())).thenReturn(Optional.of(openTicket));
        when(accessGuard.currentAccountId()).thenReturn(handlerAccountId);
        when(ticketConversationService.getRequesterTicketOriginConversation(
                direct.getConversationId(), handlerAccountId
        )).thenReturn(direct);
        when(accessGuard.ensureCanShareAsAssignee(openTicket)).thenReturn(handlerAccountId);
        when(supportTicketPortOut.save(openTicket)).thenReturn(openTicket);

        SupportTicket result = useCase.shareTicketWithConversation(
                openTicket.getSupportTicketId(), direct.getConversationId(), "conversation-share-1"
        );

        assertEquals(SupportTicketStatus.IN_PROGRESS, result.getStatus());
        assertTrue(result.getFirstRespondedAt() != null);
        verify(ticketConversationService).repostTicketCardFromConversation(
                openTicket,
                direct,
                handlerAccountId,
                SupportTicketEventType.STARTED_PROCESSING,
                SupportTicketStatus.OPEN,
                "conversation-share-1"
        );
    }

    @Test
    void resolvingTicketPostsFreshResolvedCardToPrivateConversation() {
        UUID requesterAccountId = UUID.randomUUID();
        UUID handlerAccountId = UUID.randomUUID();
        SupportTicket ticket = ticket(requesterAccountId);
        ticket.setAssignedTo(handlerAccountId);
        ticket.setStatus(SupportTicketStatus.IN_PROGRESS);
        when(supportTicketPortOut.findById(ticket.getSupportTicketId())).thenReturn(Optional.of(ticket));
        when(accessGuard.currentAccountId()).thenReturn(handlerAccountId);
        when(supportTicketPortOut.save(ticket)).thenReturn(ticket);

        SupportTicket result = useCase.resolveTicket(ticket.getSupportTicketId(), null);

        assertEquals(SupportTicketStatus.RESOLVED, result.getStatus());
        verify(ticketConversationService).postTicketStatusCard(
                eq(ticket),
                eq(handlerAccountId),
                eq(SupportTicketEventType.RESOLVED),
                eq(SupportTicketStatus.IN_PROGRESS),
                eq("resolved:" + ticket.getSupportTicketId() + ":0")
        );
    }

    @Test
    void resolvingAlreadyResolvedTicketDoesNotCreateAnotherCard() {
        UUID handlerAccountId = UUID.randomUUID();
        SupportTicket ticket = ticket(UUID.randomUUID());
        ticket.setAssignedTo(handlerAccountId);
        ticket.setStatus(SupportTicketStatus.RESOLVED);
        when(supportTicketPortOut.findById(ticket.getSupportTicketId())).thenReturn(Optional.of(ticket));
        when(accessGuard.currentAccountId()).thenReturn(handlerAccountId);

        SupportTicket result = useCase.resolveTicket(ticket.getSupportTicketId(), null);

        assertSame(ticket, result);
        assertEquals(SupportTicketStatus.RESOLVED, result.getStatus());
        verify(supportTicketPortOut, never()).save(any());
        verify(ticketConversationService, never()).postTicketStatusCard(any(), any(), any(), any(), any());
    }

    @Test
    void manualStartProgressIsRejectedBecauseClaimOwnsTheTransition() {
        assertThrows(ConflictException.class, () -> useCase.startProgress(UUID.randomUUID()));
        verify(supportTicketPortOut, never()).save(any());
    }

    @Test
    void authorizedHandlerClaimsAvailableTicketAtomically() {
        UUID ticketId = UUID.randomUUID();
        UUID handlerAccountId = UUID.randomUUID();
        SupportTicket claimed = ticket(UUID.randomUUID());
        claimed.setSupportTicketId(ticketId);
        claimed.setAssignedTo(handlerAccountId);
        claimed.setStatus(SupportTicketStatus.IN_PROGRESS);
        when(accessGuard.resolveAccountIdForClaim()).thenReturn(handlerAccountId);
        when(supportTicketPortOut.existsAssignableAccountById(handlerAccountId)).thenReturn(true);
        when(supportTicketPortOut.claimIfAvailable(ticketId, handlerAccountId)).thenReturn(true);
        when(supportTicketPortOut.findById(ticketId)).thenReturn(Optional.of(claimed));

        SupportTicket result = useCase.claimTicket(ticketId);

        assertSame(claimed, result);
        assertEquals(handlerAccountId, result.getAssignedTo());
        assertEquals(SupportTicketStatus.IN_PROGRESS, result.getStatus());
        verify(ticketConversationService).postAssistantTicketUpdate(claimed, "Yêu cầu của bạn đang được xử lý.");
    }

    @Test
    void repeatedClaimBySameAuthorizedHandlerIsIdempotent() {
        UUID ticketId = UUID.randomUUID();
        UUID handlerAccountId = UUID.randomUUID();
        SupportTicket claimed = ticket(UUID.randomUUID());
        claimed.setSupportTicketId(ticketId);
        claimed.setAssignedTo(handlerAccountId);
        claimed.setStatus(SupportTicketStatus.IN_PROGRESS);
        when(accessGuard.resolveAccountIdForClaim()).thenReturn(handlerAccountId);
        when(supportTicketPortOut.existsAssignableAccountById(handlerAccountId)).thenReturn(true);
        when(supportTicketPortOut.claimIfAvailable(ticketId, handlerAccountId)).thenReturn(false);
        when(supportTicketPortOut.findById(ticketId)).thenReturn(Optional.of(claimed));

        assertSame(claimed, useCase.claimTicket(ticketId));

        verify(ticketConversationService, never()).postAssistantTicketUpdate(any(), any());
    }

    @Test
    void claimOwnedByAnotherAuthorizedHandlerReturnsConflict() {
        UUID ticketId = UUID.randomUUID();
        UUID currentHandlerId = UUID.randomUUID();
        SupportTicket claimed = ticket(UUID.randomUUID());
        claimed.setSupportTicketId(ticketId);
        claimed.setAssignedTo(UUID.randomUUID());
        claimed.setStatus(SupportTicketStatus.IN_PROGRESS);
        when(accessGuard.resolveAccountIdForClaim()).thenReturn(currentHandlerId);
        when(supportTicketPortOut.existsAssignableAccountById(currentHandlerId)).thenReturn(true);
        when(supportTicketPortOut.claimIfAvailable(ticketId, currentHandlerId)).thenReturn(false);
        when(supportTicketPortOut.findById(ticketId)).thenReturn(Optional.of(claimed));

        assertThrows(ConflictException.class, () -> useCase.claimTicket(ticketId));
    }

    private SupportRequesterContext requester(UUID accountId) {
        return new SupportRequesterContext(
                accountId, "CUSTOMER", "Requester", "requester@example.com",
                Set.of(), Set.of(), Set.of()
        );
    }

    private ChatConversation assistantConversation(UUID accountId) {
        ChatConversation conversation = new ChatConversation();
        conversation.setConversationId(UUID.randomUUID());
        conversation.setOwnerAccountId(accountId);
        conversation.setConversationType(ChatConversationType.ASSISTANT_SUPPORT);
        return conversation;
    }

    private SupportTicketCategory category(UUID categoryId) {
        SupportTicketCategory category = new SupportTicketCategory();
        category.setCategoryId(categoryId);
        category.setFeatureCode("ACCOUNT_ACCESS");
        return category;
    }

    private SupportTicket ticket(UUID requesterAccountId) {
        SupportTicket ticket = new SupportTicket();
        ticket.setSupportTicketId(UUID.randomUUID());
        ticket.setRequesterAccountId(requesterAccountId);
        ticket.setRequesterRoleCode("CUSTOMER");
        ticket.setCategoryId(UUID.randomUUID());
        ticket.setTitle("Barrier gate support");
        ticket.setContent("The barrier gate cannot be opened");
        ticket.setStatus(SupportTicketStatus.OPEN);
        ticket.setReopenCount(0);
        return ticket;
    }
}
