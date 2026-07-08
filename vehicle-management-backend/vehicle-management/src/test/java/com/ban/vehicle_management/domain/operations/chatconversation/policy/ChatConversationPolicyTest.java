package com.ban.vehicle_management.domain.operations.chatconversation.policy;

import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChatConversationPolicyTest {

    private final ChatConversationPolicy policy = new ChatConversationPolicy();

    @Test
    void initializeInternalDirectRejectsSelfChat() {
        UUID accountId = UUID.randomUUID();
        ChatConversation conversation = new ChatConversation();

        assertThrows(
                BadRequestException.class,
                () -> policy.initializeInternalDirect(conversation, accountId, accountId)
        );
    }

    @Test
    void initializeInternalDirectSetsActiveTypeAndOwner() {
        UUID ownerId = UUID.randomUUID();
        ChatConversation conversation = new ChatConversation();
        conversation.setTitle("  Ca toi  ");

        policy.initializeInternalDirect(conversation, ownerId, UUID.randomUUID());

        assertEquals(ChatConversationType.INTERNAL_DIRECT, conversation.getConversationType());
        assertEquals(ChatConversationStatus.ACTIVE, conversation.getStatus());
        assertEquals(ownerId, conversation.getOwnerAccountId());
        assertEquals("Ca toi", conversation.getTitle());
    }
}
