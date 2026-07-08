package com.ban.vehicle_management.entrypoint.dto.operations.chatconversation.response;

import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationType;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatConversationUserResponse {
    private UUID conversationId;
    private ChatConversationType conversationType;
    private String title;
    private ChatConversationStatus status;
    private UUID customerId;
    private UUID supportTicketId;
    private UUID ownerAccountId;
    private UUID assignedTo;
    private String relatedSchema;
    private String relatedTable;
    private UUID relatedId;
    private UUID lastMessageId;
    private String lastMessageAt;
}
