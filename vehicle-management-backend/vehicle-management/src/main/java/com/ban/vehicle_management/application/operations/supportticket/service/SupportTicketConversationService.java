package com.ban.vehicle_management.application.operations.supportticket.service;

import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatConversationPortOut;
import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatRealtimeEventPublisherPortOut;
import com.ban.vehicle_management.application.operations.chatconversation.mapper.ChatRealtimeEventMapper;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketConversationLinkPortOut;
import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketEventPortOut;
import com.ban.vehicle_management.application.operations.supportticket.mapper.SupportTicketSnapshotJsonMapper;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversationMember;
import com.ban.vehicle_management.domain.operations.chatconversation.policy.ChatConversationPolicy;
import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessage;
import com.ban.vehicle_management.domain.operations.chatmessage.policy.ChatMessagePolicy;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicketEvent;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationType;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ChatMemberRole;
import com.ban.vehicle_management.shared.enumeration.operations.ChatMemberStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketConversationLinkReason;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketEventType;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import com.ban.vehicle_management.shared.transaction.TransactionalEvents;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Bridges a support ticket into the private requester-to-system-admin conversation.
 * A ticket never owns a conversation: the message card is the auditable link.
 */
@Component
public class SupportTicketConversationService {

    private static final Set<String> ASSIGNED_HANDLER_PERMISSIONS = Set.of(
            "SUPPORT_TICKET_READ_ALL",
            "SUPPORT_TICKET_PROCESS_ALL",
            "SUPPORT_TICKET_RESPOND_ASSIGNED"
    );

    private final ChatConversationPortOut chatPortOut;
    private final SupportTicketConversationLinkPortOut ticketConversationLinkPortOut;
    private final SupportTicketEventPortOut ticketEventPortOut;
    private final SupportTicketSnapshotJsonMapper snapshotJsonMapper;
    private final ChatRealtimeEventPublisherPortOut realtimeEventPublisher;
    private final ChatRealtimeEventMapper realtimeEventMapper;
    private final ChatConversationPolicy conversationPolicy = new ChatConversationPolicy();
    private final ChatMessagePolicy messagePolicy = new ChatMessagePolicy();

    public SupportTicketConversationService(
            ChatConversationPortOut chatPortOut,
            SupportTicketConversationLinkPortOut ticketConversationLinkPortOut,
            SupportTicketEventPortOut ticketEventPortOut,
            SupportTicketSnapshotJsonMapper snapshotJsonMapper,
            ChatRealtimeEventPublisherPortOut realtimeEventPublisher,
            ChatRealtimeEventMapper realtimeEventMapper
    ) {
        this.chatPortOut = chatPortOut;
        this.ticketConversationLinkPortOut = ticketConversationLinkPortOut;
        this.ticketEventPortOut = ticketEventPortOut;
        this.snapshotJsonMapper = snapshotJsonMapper;
        this.realtimeEventPublisher = realtimeEventPublisher;
        this.realtimeEventMapper = realtimeEventMapper;
    }

    public ChatConversation openRequesterConversationForReply(
            SupportTicket ticket,
            UUID assignedAccountId,
            SupportTicketStatus previousStatus,
            String idempotencyKey
    ) {
        chatPortOut.lockRequesterSupport(ticket.getRequesterAccountId());
        ChatConversation conversation = chatPortOut.findActiveRequesterSupportConversation(
                        ticket.getRequesterAccountId(), assignedAccountId
                )
                .orElseGet(() -> createRequesterConversation(ticket.getRequesterAccountId(), assignedAccountId));
        SupportTicketConversationLinkReason linkReason = resolveLinkReason(ticket);
        if (linkReason == SupportTicketConversationLinkReason.REOPENED) {
            ticketConversationLinkPortOut.deactivate(ticket.getSupportTicketId());
        }
        ticketConversationLinkPortOut.activate(
                ticket.getSupportTicketId(),
                conversation.getConversationId(),
                linkReason,
                assignedAccountId
        );
        SupportTicketEventType eventType = previousStatus == SupportTicketStatus.OPEN
                ? SupportTicketEventType.STARTED_PROCESSING
                : SupportTicketEventType.SHARED;
        postTicketCard(conversation, ticket, assignedAccountId, eventType, previousStatus, idempotencyKey);
        return chatPortOut.findConversationById(conversation.getConversationId())
                .orElseThrow(() -> new NotFoundException("Chat conversation not found"));
    }

    /** Re-surfaces a reopened ticket in its existing private conversation when the actor is a member. */
    public void repostReopenedTicketCard(
            SupportTicket ticket,
            UUID reopenedByAccountId,
            SupportTicketStatus previousStatus,
            String idempotencyKey
    ) {
        ticketConversationLinkPortOut.findActiveBySupportTicketId(ticket.getSupportTicketId())
                .flatMap(link -> chatPortOut.findConversationById(link.getConversationId()))
                .filter(conversation -> conversation.getConversationType() == ChatConversationType.SUPPORT_REQUESTER_DIRECT)
                .filter(conversation -> ticket.getRequesterAccountId().equals(conversation.getOwnerAccountId()))
                .filter(conversation -> chatPortOut.existsActiveMember(conversation.getConversationId(), reopenedByAccountId))
                .ifPresent(conversation -> {
                    ticketConversationLinkPortOut.deactivate(ticket.getSupportTicketId());
                    ticketConversationLinkPortOut.activate(
                            ticket.getSupportTicketId(),
                            conversation.getConversationId(),
                            SupportTicketConversationLinkReason.REOPENED,
                            reopenedByAccountId
                    );
                    postTicketCard(
                            conversation,
                            ticket,
                            reopenedByAccountId,
                            SupportTicketEventType.REOPENED,
                            previousStatus,
                            idempotencyKey
                    );
                });
    }

    public ChatConversation getActiveRequesterConversation(SupportTicket ticket, UUID requesterAccountId) {
        UUID conversationId = ticketConversationLinkPortOut.findActiveBySupportTicketId(ticket.getSupportTicketId())
                .map(link -> link.getConversationId())
                .orElseThrow(() -> new NotFoundException("Requester support conversation has not started"));
        if (!chatPortOut.existsActiveMember(conversationId, requesterAccountId)) {
            throw new org.springframework.security.access.AccessDeniedException("Access is denied");
        }
        return chatPortOut.findConversationById(conversationId)
                .orElseThrow(() -> new NotFoundException("Chat conversation not found"));
    }

    public ChatConversation openOrCreateAssistantConversation(UUID requesterAccountId) {
        chatPortOut.lockRequesterSupport(requesterAccountId);
        ChatConversation conversation = chatPortOut.findAssistantSupportConversationByOwnerAccountId(requesterAccountId)
                .orElseGet(() -> createAssistantConversation(requesterAccountId));
        if (conversation.getStatus() != ChatConversationStatus.ACTIVE
                || !"Tr\u1ee3 l\u00fd h\u1ed7 tr\u1ee3 CoParking".equals(conversation.getTitle())) {
            conversation.setStatus(ChatConversationStatus.ACTIVE);
            conversation.setTitle("Tr\u1ee3 l\u00fd h\u1ed7 tr\u1ee3 CoParking");
            conversation = chatPortOut.saveConversation(conversation);
        }
        saveActiveMember(conversation.getConversationId(), requesterAccountId, ChatMemberRole.OWNER);
        return chatPortOut.findConversationById(conversation.getConversationId())
                .orElseThrow(() -> new NotFoundException("Chat conversation not found"));
    }

    /** Adds a requester's subsequent intake submission to the existing active ticket thread. */
    public void appendRequesterIntakeMessage(ChatConversation conversation, UUID requesterAccountId, String content) {
        ChatMessage message = new ChatMessage();
        message.setMessageId(UUID.randomUUID());
        message.setConversationId(conversation.getConversationId());
        message.setSenderAccountId(requesterAccountId);
        message.setContent(content);
        messagePolicy.initializeText(message);
        ChatMessage savedMessage = chatPortOut.saveMessage(message);
        conversation.setLastMessageId(savedMessage.getMessageId());
        conversation.setLastMessageAt(savedMessage.getCreatedAt() == null ? Instant.now() : savedMessage.getCreatedAt());
        chatPortOut.saveConversation(conversation);
    }

    public void postTicketCardFromRequester(SupportTicket ticket, ChatConversation conversation, UUID requesterAccountId) {
        boolean supportedOrigin = conversation.getConversationType() == ChatConversationType.SUPPORT_REQUESTER_DIRECT
                || conversation.getConversationType() == ChatConversationType.ASSISTANT_SUPPORT;
        if (!supportedOrigin || !chatPortOut.existsActiveMember(conversation.getConversationId(), requesterAccountId)) {
            throw new BadRequestException("Support ticket must be created from an active requester conversation");
        }
        postTicketCard(
                conversation,
                ticket,
                requesterAccountId,
                SupportTicketEventType.SHARED,
                ticket.getStatus(),
                "initial:" + ticket.getSupportTicketId()
        );
    }

    public void repostTicketCardFromRequester(
            SupportTicket ticket,
            ChatConversation conversation,
            UUID requesterAccountId,
            String idempotencyKey
    ) {
        boolean supportedOrigin = conversation.getConversationType() == ChatConversationType.SUPPORT_REQUESTER_DIRECT
                || conversation.getConversationType() == ChatConversationType.ASSISTANT_SUPPORT;
        if (!supportedOrigin || !chatPortOut.existsActiveMember(conversation.getConversationId(), requesterAccountId)) {
            throw new BadRequestException("Support ticket must be shared in an active requester conversation");
        }
        postTicketCard(
                conversation,
                ticket,
                requesterAccountId,
                SupportTicketEventType.SHARED,
                ticket.getStatus(),
                idempotencyKey
        );
    }

    public void repostTicketCardFromConversation(
            SupportTicket ticket,
            ChatConversation conversation,
            UUID senderAccountId,
            SupportTicketEventType eventType,
            SupportTicketStatus previousStatus,
            String idempotencyKey
    ) {
        boolean supportedOrigin = conversation.getConversationType() == ChatConversationType.SUPPORT_REQUESTER_DIRECT
                || conversation.getConversationType() == ChatConversationType.ASSISTANT_SUPPORT;
        if (!supportedOrigin
                || !ticket.getRequesterAccountId().equals(conversation.getOwnerAccountId())
                || !chatPortOut.existsActiveMember(conversation.getConversationId(), senderAccountId)) {
            throw new BadRequestException("Support ticket can only be shared in its active requester conversation");
        }
        if (conversation.getConversationType() == ChatConversationType.SUPPORT_REQUESTER_DIRECT) {
            if (ticket.getAssignedTo() == null
                    || !chatPortOut.existsActiveMember(conversation.getConversationId(), ticket.getAssignedTo())) {
                throw new BadRequestException("Support ticket must be shared with its assigned handler");
            }
            ticketConversationLinkPortOut.deactivate(ticket.getSupportTicketId());
            ticketConversationLinkPortOut.activate(
                    ticket.getSupportTicketId(),
                    conversation.getConversationId(),
                    resolveLinkReason(ticket),
                    senderAccountId
            );
        }
        postTicketCard(conversation, ticket, senderAccountId, eventType, previousStatus, idempotencyKey);
    }

    public void postTicketStatusCard(
            SupportTicket ticket,
            UUID senderAccountId,
            SupportTicketEventType eventType,
            SupportTicketStatus previousStatus,
            String idempotencyKey
    ) {
        ticketConversationLinkPortOut.findActiveBySupportTicketId(ticket.getSupportTicketId())
                .flatMap(link -> chatPortOut.findConversationById(link.getConversationId()))
                .filter(conversation -> conversation.getConversationType() == ChatConversationType.SUPPORT_REQUESTER_DIRECT)
                .filter(conversation -> ticket.getRequesterAccountId().equals(conversation.getOwnerAccountId()))
                .filter(conversation -> chatPortOut.existsActiveMember(conversation.getConversationId(), senderAccountId))
                .ifPresent(conversation -> postTicketCard(
                        conversation,
                        ticket,
                        senderAccountId,
                        eventType,
                        previousStatus,
                        idempotencyKey
                ));
    }

    /** Posts a lifecycle snapshot generated by the system, without requiring a human sender membership. */
    public void postSystemTicketStatusCard(
            SupportTicket ticket,
            SupportTicketEventType eventType,
            SupportTicketStatus previousStatus,
            String idempotencyKey
    ) {
        ticketConversationLinkPortOut.findActiveBySupportTicketId(ticket.getSupportTicketId())
                .flatMap(link -> chatPortOut.findConversationById(link.getConversationId()))
                .filter(conversation -> conversation.getConversationType() == ChatConversationType.SUPPORT_REQUESTER_DIRECT)
                .filter(conversation -> ticket.getRequesterAccountId().equals(conversation.getOwnerAccountId()))
                .ifPresent(conversation -> postTicketCard(
                        conversation,
                        ticket,
                        null,
                        eventType,
                        previousStatus,
                        idempotencyKey
                ));
    }

    public ChatConversation getConversationForHistory(UUID conversationId) {
        return chatPortOut.findConversationById(conversationId)
                .orElseThrow(() -> new NotFoundException("Chat conversation not found"));
    }

    public ChatConversation getRequesterTicketOriginConversation(UUID conversationId, UUID requesterAccountId) {
        ChatConversation conversation = chatPortOut.findConversationById(conversationId)
                .orElseThrow(() -> new NotFoundException("Chat conversation not found"));
        boolean supportedOrigin = conversation.getConversationType() == ChatConversationType.SUPPORT_REQUESTER_DIRECT
                || conversation.getConversationType() == ChatConversationType.ASSISTANT_SUPPORT;
        if (!supportedOrigin
                || !chatPortOut.existsActiveMember(conversationId, requesterAccountId)) {
            throw new BadRequestException("Conversation must be an active requester support conversation");
        }
        return conversation;
    }

    public UUID resolveCounterpartAccountId(ChatConversation conversation, UUID requesterAccountId) {
        if (conversation.getConversationType() != ChatConversationType.SUPPORT_REQUESTER_DIRECT
                || !chatPortOut.existsActiveMember(conversation.getConversationId(), requesterAccountId)) {
            throw new BadRequestException("Conversation must be an active private requester conversation");
        }
        return chatPortOut.findActiveMemberAccountIds(conversation.getConversationId()).stream()
                .filter(accountId -> !accountId.equals(requesterAccountId))
                .filter(accountId -> chatPortOut.existsActiveAccountWithPermissions(accountId, ASSIGNED_HANDLER_PERMISSIONS))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Active support staff was not found in conversation"));
    }

    private ChatConversation createRequesterConversation(UUID requesterAccountId, UUID assignedAccountId) {
        if (!chatPortOut.existsActiveAccountWithPermissions(assignedAccountId, ASSIGNED_HANDLER_PERMISSIONS)) {
            throw new NotFoundException("Assigned support staff was not found");
        }

        ChatConversation conversation = new ChatConversation();
        conversation.setConversationId(UUID.randomUUID());
        conversationPolicy.initializeRequesterSupport(conversation, requesterAccountId);
        conversation.setAssignedTo(assignedAccountId);
        ChatConversation savedConversation = chatPortOut.saveConversation(conversation);
        saveActiveMember(savedConversation.getConversationId(), requesterAccountId, ChatMemberRole.OWNER);
        saveActiveMember(savedConversation.getConversationId(), assignedAccountId, ChatMemberRole.ASSIGNEE);
        return savedConversation;
    }

    private ChatConversation createAssistantConversation(UUID requesterAccountId) {
        ChatConversation conversation = new ChatConversation();
        conversation.setConversationId(UUID.randomUUID());
        conversation.setTitle("Tr\u1ee3 l\u00fd h\u1ed7 tr\u1ee3 CoParking");
        conversationPolicy.initializeRequesterAssistant(conversation, requesterAccountId);
        ChatConversation savedConversation = chatPortOut.saveConversation(conversation);
        saveActiveMember(savedConversation.getConversationId(), requesterAccountId, ChatMemberRole.OWNER);
        return savedConversation;
    }

    private ChatMessage postTicketCard(
            ChatConversation conversation,
            SupportTicket ticket,
            UUID senderAccountId,
            SupportTicketEventType eventType,
            SupportTicketStatus previousStatus,
            String idempotencyKey
    ) {
        String normalizedKey = TextValidationUtils.normalizeRequiredText(
                idempotencyKey,
                "idempotencyKey",
                100
        );
        chatPortOut.lockConversation(conversation.getConversationId());
        SupportTicketEvent existingEvent = ticketEventPortOut
                .findByConversationIdAndIdempotencyKey(conversation.getConversationId(), normalizedKey)
                .orElse(null);
        if (existingEvent != null) {
            if (!ticket.getSupportTicketId().equals(existingEvent.getSupportTicketId())) {
                throw new BadRequestException("Idempotency key belongs to another support ticket event");
            }
            return chatPortOut.findMessageBySupportTicketEventId(existingEvent.getEventId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Support ticket event is missing its chat message"
                    ));
        }

        Instant occurredAt = Instant.now();
        UUID eventId = UUID.randomUUID();
        String snapshotPayload = snapshotJsonMapper.toJson(
                eventId,
                eventType,
                ticket,
                senderAccountId,
                occurredAt
        );
        SupportTicketEvent event = new SupportTicketEvent();
        event.setEventId(eventId);
        event.setSupportTicketId(ticket.getSupportTicketId());
        event.setConversationId(conversation.getConversationId());
        event.setActorAccountId(senderAccountId);
        event.setEventType(eventType);
        event.setFromStatus(previousStatus);
        event.setToStatus(ticket.getStatus());
        event.setSnapshotStatus(ticket.getStatus());
        event.setSnapshotPayload(snapshotPayload);
        event.setIdempotencyKey(normalizedKey);
        ticketEventPortOut.save(event);

        ChatMessage card = new ChatMessage();
        card.setMessageId(UUID.randomUUID());
        card.setConversationId(conversation.getConversationId());
        card.setSenderAccountId(senderAccountId);
        card.setContent(ticket.getContent());
        card.setRelatedSchema("operations");
        card.setRelatedTable("support_tickets");
        card.setRelatedId(ticket.getSupportTicketId());
        card.setSupportTicketEventId(eventId);
        card.setMetadata(snapshotPayload);
        messagePolicy.initializeSupportRequest(card);
        ChatMessage savedCard = chatPortOut.saveMessage(card);

        conversation.setLastMessageId(savedCard.getMessageId());
        conversation.setLastMessageAt(savedCard.getCreatedAt() == null ? Instant.now() : savedCard.getCreatedAt());
        chatPortOut.saveConversation(conversation);
        publishAfterCommit(savedCard);
        return savedCard;
    }

    public void postAssistantTicketUpdate(SupportTicket ticket, String content) {
        chatPortOut.findActiveAssistantSupportConversationByOwnerAccountId(ticket.getRequesterAccountId()).ifPresent(conversation -> {
            ChatMessage message = new ChatMessage();
            message.setMessageId(UUID.randomUUID());
            message.setConversationId(conversation.getConversationId());
            message.setContent(content);
            message.setRelatedSchema("operations");
            message.setRelatedTable("support_tickets");
            message.setRelatedId(ticket.getSupportTicketId());
            messagePolicy.initializeSystem(message);
            ChatMessage savedMessage = chatPortOut.saveMessage(message);
            conversation.setLastMessageId(savedMessage.getMessageId());
            conversation.setLastMessageAt(savedMessage.getCreatedAt() == null ? Instant.now() : savedMessage.getCreatedAt());
            chatPortOut.saveConversation(conversation);
            publishAfterCommit(savedMessage);
        });
    }

    private SupportTicketConversationLinkReason resolveLinkReason(SupportTicket ticket) {
        Instant lastLinkAt = ticketConversationLinkPortOut.findMostRecentBySupportTicketId(ticket.getSupportTicketId())
                .map(link -> link.getLinkedAt())
                .orElse(null);
        if (ticket.getLastReopenedAt() != null
                && (lastLinkAt == null || ticket.getLastReopenedAt().isAfter(lastLinkAt))) {
            return SupportTicketConversationLinkReason.REOPENED;
        }
        return ticketConversationLinkPortOut.existsBySupportTicketId(ticket.getSupportTicketId())
                ? SupportTicketConversationLinkReason.REASSIGNED
                : SupportTicketConversationLinkReason.FIRST_REPLY;
    }

    private void publishAfterCommit(ChatMessage message) {
        TransactionalEvents.runAfterCommit(() -> realtimeEventPublisher.publish(
                realtimeEventMapper.toRealtimeEvent(message, Instant.now())
        ));
    }

    private void saveActiveMember(UUID conversationId, UUID accountId, ChatMemberRole role) {
        ChatConversationMember member = chatPortOut.findMember(conversationId, accountId)
                .orElseGet(ChatConversationMember::new);
        if (member.getConversationMemberId() == null) {
            member.setConversationMemberId(UUID.randomUUID());
            member.setConversationId(conversationId);
            member.setAccountId(accountId);
            member.setJoinedAt(Instant.now());
        }
        member.setMemberRole(role);
        member.setStatus(ChatMemberStatus.ACTIVE);
        member.setLeftAt(null);
        chatPortOut.saveMember(member);
    }
}
