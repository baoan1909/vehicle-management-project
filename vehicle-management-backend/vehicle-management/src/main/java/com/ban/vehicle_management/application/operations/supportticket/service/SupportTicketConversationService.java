package com.ban.vehicle_management.application.operations.supportticket.service;

import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatConversationPortOut;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversationMember;
import com.ban.vehicle_management.domain.operations.chatconversation.policy.ChatConversationPolicy;
import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessage;
import com.ban.vehicle_management.domain.operations.chatmessage.policy.ChatMessagePolicy;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationType;
import com.ban.vehicle_management.shared.enumeration.operations.ChatMemberRole;
import com.ban.vehicle_management.shared.enumeration.operations.ChatMemberStatus;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.exception.NotFoundException;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Bridges a support ticket into the private customer-to-assignee conversation.
 * A ticket never owns a conversation: the message card is the auditable link.
 */
@Component
public class SupportTicketConversationService {

    private static final Set<String> ASSIGNED_HANDLER_PERMISSIONS = Set.of(
            "SUPPORT_TICKET_READ_ASSIGNED",
            "SUPPORT_TICKET_PROCESS_ASSIGNED",
            "SUPPORT_TICKET_RESPOND_ASSIGNED"
    );

    private final ChatConversationPortOut chatPortOut;
    private final ChatConversationPolicy conversationPolicy = new ChatConversationPolicy();
    private final ChatMessagePolicy messagePolicy = new ChatMessagePolicy();

    public SupportTicketConversationService(ChatConversationPortOut chatPortOut) {
        this.chatPortOut = chatPortOut;
    }

    public ChatConversation openPrivateConversationForReply(SupportTicket ticket, UUID assignedAccountId) {
        ChatConversation conversation = chatPortOut.findActiveCustomerSupportConversation(ticket.getCustomerId(), assignedAccountId)
                .orElseGet(() -> createPrivateConversation(ticket.getCustomerId(), assignedAccountId));
        postTicketCardIfMissing(conversation, ticket, assignedAccountId);
        return chatPortOut.findConversationById(conversation.getConversationId())
                .orElseThrow(() -> new NotFoundException("Chat conversation not found"));
    }

    public void postTicketCardFromCustomer(SupportTicket ticket, ChatConversation conversation, UUID customerAccountId) {
        if (conversation.getConversationType() != ChatConversationType.CUSTOMER_DIRECT
                || !chatPortOut.existsActiveMember(conversation.getConversationId(), customerAccountId)) {
            throw new BadRequestException("Support ticket must be created from an active private customer conversation");
        }
        postTicketCardIfMissing(conversation, ticket, customerAccountId);
    }

    public ChatConversation getPrivateCustomerConversation(UUID conversationId, UUID customerAccountId) {
        ChatConversation conversation = chatPortOut.findConversationById(conversationId)
                .orElseThrow(() -> new NotFoundException("Chat conversation not found"));
        if (conversation.getConversationType() != ChatConversationType.CUSTOMER_DIRECT
                || !chatPortOut.existsActiveMember(conversationId, customerAccountId)) {
            throw new BadRequestException("Conversation must be an active private customer conversation");
        }
        return conversation;
    }

    public UUID resolveCounterpartAccountId(ChatConversation conversation, UUID customerAccountId) {
        if (conversation.getConversationType() != ChatConversationType.CUSTOMER_DIRECT
                || !chatPortOut.existsActiveMember(conversation.getConversationId(), customerAccountId)) {
            throw new BadRequestException("Conversation must be an active private customer conversation");
        }
        return chatPortOut.findActiveMemberAccountIds(conversation.getConversationId()).stream()
                .filter(accountId -> !accountId.equals(customerAccountId))
                .filter(accountId -> chatPortOut.existsActiveAccountWithPermissions(accountId, ASSIGNED_HANDLER_PERMISSIONS))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Active support staff was not found in conversation"));
    }

    private ChatConversation createPrivateConversation(UUID customerId, UUID assignedAccountId) {
        UUID customerAccountId = customerAccountId(customerId);
        if (!chatPortOut.existsActiveAccountWithPermissions(assignedAccountId, ASSIGNED_HANDLER_PERMISSIONS)) {
            throw new NotFoundException("Assigned support staff was not found");
        }

        ChatConversation conversation = new ChatConversation();
        conversation.setConversationId(UUID.randomUUID());
        conversationPolicy.initializeCustomerSupport(conversation, assignedAccountId, customerId);
        conversation.setAssignedTo(assignedAccountId);
        ChatConversation savedConversation = chatPortOut.saveConversation(conversation);
        saveActiveMember(savedConversation.getConversationId(), assignedAccountId, ChatMemberRole.OWNER);
        saveActiveMember(savedConversation.getConversationId(), customerAccountId, ChatMemberRole.CUSTOMER);
        return savedConversation;
    }

    private void postTicketCardIfMissing(ChatConversation conversation, SupportTicket ticket, UUID senderAccountId) {
        if (chatPortOut.existsSupportTicketCard(conversation.getConversationId(), ticket.getSupportTicketId())) {
            return;
        }

        ChatMessage card = new ChatMessage();
        card.setMessageId(UUID.randomUUID());
        card.setConversationId(conversation.getConversationId());
        card.setSenderAccountId(senderAccountId);
        card.setContent(ticket.getContent());
        card.setRelatedSchema("operations");
        card.setRelatedTable("support_tickets");
        card.setRelatedId(ticket.getSupportTicketId());
        messagePolicy.initializeSupportRequest(card);
        ChatMessage savedCard = chatPortOut.saveMessage(card);

        conversation.setLastMessageId(savedCard.getMessageId());
        conversation.setLastMessageAt(savedCard.getCreatedAt() == null ? Instant.now() : savedCard.getCreatedAt());
        chatPortOut.saveConversation(conversation);
    }

    private UUID customerAccountId(UUID customerId) {
        return chatPortOut.findAccountIdByCustomerId(customerId)
                .orElseThrow(() -> new NotFoundException("Customer account not found"));
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
