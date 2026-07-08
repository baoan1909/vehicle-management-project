package com.ban.vehicle_management.domain.operations.chatconversation.model;

import com.ban.vehicle_management.domain.operations.chatmessage.model.ChatMessage;

public record ChatInboxItem(
        ChatConversation conversation,
        ChatMessage lastMessage,
        long unreadCount
) {
}
