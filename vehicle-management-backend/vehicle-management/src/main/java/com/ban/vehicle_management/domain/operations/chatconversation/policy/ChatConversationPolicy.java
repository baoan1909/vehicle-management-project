package com.ban.vehicle_management.domain.operations.chatconversation.policy;

import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import com.ban.vehicle_management.shared.utils.TextValidationUtils;
import java.util.Set;
import java.util.UUID;

public class ChatConversationPolicy {

    public void initializeInternalDirect(ChatConversation conversation, UUID ownerAccountId, UUID targetAccountId) {
        if (ownerAccountId == null || targetAccountId == null) {
            throw new BadRequestException("targetAccountId must not be null");
        }
        if (ownerAccountId.equals(targetAccountId)) {
            throw new BadRequestException("Cannot create direct conversation with yourself");
        }
        initialize(conversation, ChatConversationType.INTERNAL_DIRECT, ownerAccountId);
    }

    public void initializeInternalGroup(ChatConversation conversation, UUID ownerAccountId, Set<UUID> memberAccountIds) {
        if (memberAccountIds == null || memberAccountIds.size() < 2) {
            throw new BadRequestException("Internal group conversation must have at least two members");
        }
        initialize(conversation, ChatConversationType.INTERNAL_GROUP, ownerAccountId);
    }

    public void initializeCustomerSupport(ChatConversation conversation, UUID ownerAccountId, UUID customerId) {
        if (customerId == null) {
            throw new BadRequestException("customerId must not be null");
        }
        initialize(conversation, ChatConversationType.CUSTOMER_DIRECT, ownerAccountId);
        conversation.setCustomerId(customerId);
    }

    /**
     * Personal, long-lived support-assistant conversation. Staff never become participants
     * of this conversation; human assistance uses a separate CUSTOMER_DIRECT conversation.
     */
    public void initializeAssistantSupport(ChatConversation conversation, UUID customerAccountId, UUID customerId) {
        if (customerId == null || customerAccountId == null) {
            throw new BadRequestException("customerId and customerAccountId must not be null");
        }
        initialize(conversation, ChatConversationType.ASSISTANT_SUPPORT, customerAccountId);
        conversation.setCustomerId(customerId);
    }

    public void initializeSupportTicket(ChatConversation conversation, UUID ownerAccountId, UUID customerId, UUID supportTicketId) {
        if (customerId == null || supportTicketId == null) {
            throw new BadRequestException("customerId and supportTicketId must not be null");
        }
        initialize(conversation, ChatConversationType.SUPPORT_TICKET, ownerAccountId);
        conversation.setCustomerId(customerId);
        conversation.setSupportTicketId(supportTicketId);
        conversation.setRelatedSchema("operations");
        conversation.setRelatedTable("support_tickets");
        conversation.setRelatedId(supportTicketId);
    }

    public void ensureCanReceiveUserMessage(ChatConversation conversation) {
        if (conversation == null) {
            throw new BadRequestException("conversation must not be null");
        }
        if (conversation.getStatus() == ChatConversationStatus.CLOSED) {
            throw new BadRequestException("Closed conversation cannot receive new user messages");
        }
    }

    private void initialize(ChatConversation conversation, ChatConversationType type, UUID ownerAccountId) {
        if (conversation == null) {
            throw new BadRequestException("conversation must not be null");
        }
        conversation.setConversationType(type);
        conversation.setStatus(ChatConversationStatus.ACTIVE);
        conversation.setOwnerAccountId(ownerAccountId);
        conversation.setTitle(TextValidationUtils.normalizeNullableText(conversation.getTitle(), "title", 200));
    }
}
