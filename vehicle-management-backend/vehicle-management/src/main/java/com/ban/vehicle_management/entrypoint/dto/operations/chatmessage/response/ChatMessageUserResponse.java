package com.ban.vehicle_management.entrypoint.dto.operations.chatmessage.response;

import com.ban.vehicle_management.shared.enumeration.operations.ChatMessageType;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatMessageUserResponse {
    private UUID messageId;
    private UUID conversationId;
    private UUID senderAccountId;
    private ChatMessageType messageType;
    private String content;
    private UUID replyToMessageId;
    private String relatedSchema;
    private String relatedTable;
    private UUID relatedId;
    private UUID contextTicketId;
    private boolean deleted;
    private java.time.Instant deletedAt;
    private java.time.Instant editedAt;
    private java.time.Instant createdAt;
    private List<ChatAttachmentUserResponse> attachments = new ArrayList<>();
}
