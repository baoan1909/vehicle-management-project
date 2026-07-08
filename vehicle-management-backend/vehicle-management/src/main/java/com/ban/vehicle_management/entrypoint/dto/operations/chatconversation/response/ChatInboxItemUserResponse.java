package com.ban.vehicle_management.entrypoint.dto.operations.chatconversation.response;

import com.ban.vehicle_management.entrypoint.dto.operations.chatmessage.response.ChatMessageUserResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatInboxItemUserResponse {
    private ChatConversationUserResponse conversation;
    private ChatMessageUserResponse lastMessage;
    private long unreadCount;
}
