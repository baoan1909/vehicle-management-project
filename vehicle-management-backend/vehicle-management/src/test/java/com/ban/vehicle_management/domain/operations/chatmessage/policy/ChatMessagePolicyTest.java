package com.ban.vehicle_management.domain.operations.chatmessage.policy;

import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessage;
import com.ban.vehicle_management.shared.enumeration.operations.ChatMessageType;
import com.ban.vehicle_management.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChatMessagePolicyTest {

    private final ChatMessagePolicy policy = new ChatMessagePolicy();

    @Test
    void initializeTextNormalizesContent() {
        ChatMessage message = new ChatMessage();
        message.setContent("  Xin chao  ");

        policy.initializeText(message);

        assertEquals(ChatMessageType.TEXT, message.getMessageType());
        assertEquals("Xin chao", message.getContent());
        assertFalse(message.isDeleted());
    }

    @Test
    void initializeTextRejectsBlankContent() {
        ChatMessage message = new ChatMessage();
        message.setContent("   ");

        assertThrows(BadRequestException.class, () -> policy.initializeText(message));
    }

    @Test
    void initializeTextRejectsUnsupportedCharacters() {
        ChatMessage message = new ChatMessage();
        message.setContent("hello <script>");

        assertThrows(BadRequestException.class, () -> policy.initializeText(message));
    }
}
