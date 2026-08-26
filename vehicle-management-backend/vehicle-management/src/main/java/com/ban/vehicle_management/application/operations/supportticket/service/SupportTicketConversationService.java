package com.ban.vehicle_management.application.operations.supportticket.service;

import com.ban.vehicle_management.application.operations.chatconversation.port.out.ChatConversationPortOut;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversationMember;
import com.ban.vehicle_management.domain.operations.chatconversation.policy.ChatConversationPolicy;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ChatMemberRole;
import com.ban.vehicle_management.shared.enumeration.operations.ChatMemberStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Keeps the operational conversation and the support-ticket workflow in one transaction. */
@Component
public class SupportTicketConversationService {

    private final ChatConversationPortOut chatPortOut;
    private final ChatConversationPolicy conversationPolicy = new ChatConversationPolicy();

    public SupportTicketConversationService(ChatConversationPortOut chatPortOut) {
        this.chatPortOut = chatPortOut;
    }

    public void createForTicket(SupportTicket ticket) {
        if (chatPortOut.findActiveSupportTicketConversation(ticket.getSupportTicketId()).isPresent()) {
            return;
        }

        UUID customerAccountId = chatPortOut.findAccountIdByCustomerId(ticket.getCustomerId()).orElse(null);
        if (customerAccountId == null) {
            return;
        }

        ChatConversation conversation = new ChatConversation();
        conversation.setConversationId(UUID.randomUUID());
        conversation.setTitle(ticket.getTitle());
        conversationPolicy.initializeSupportTicket(
                conversation,
                customerAccountId,
                ticket.getCustomerId(),
                ticket.getSupportTicketId()
        );
        ChatConversation savedConversation = chatPortOut.saveConversation(conversation);
        saveActiveMember(savedConversation.getConversationId(), customerAccountId, ChatMemberRole.CUSTOMER);
    }

    public void syncAssignment(SupportTicket ticket) {
        chatPortOut.findActiveSupportTicketConversation(ticket.getSupportTicketId()).ifPresent(conversation -> {
            conversation.setAssignedTo(ticket.getAssignedTo());
            chatPortOut.saveConversation(conversation);
            if (ticket.getAssignedTo() != null) {
                saveActiveMember(conversation.getConversationId(), ticket.getAssignedTo(), ChatMemberRole.ASSIGNEE);
            }
        });
    }

    public void syncStatus(SupportTicket ticket) {
        chatPortOut.findActiveSupportTicketConversation(ticket.getSupportTicketId()).ifPresent(conversation -> {
            conversation.setStatus(ticket.getStatus().name().equals("CLOSED")
                    ? ChatConversationStatus.CLOSED
                    : ChatConversationStatus.ACTIVE);
            chatPortOut.saveConversation(conversation);
        });
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
