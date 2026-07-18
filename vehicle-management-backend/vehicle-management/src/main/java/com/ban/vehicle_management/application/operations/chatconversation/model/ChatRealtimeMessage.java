package com.ban.vehicle_management.application.operations.chatconversation.model;

import com.ban.vehicle_management.shared.enumeration.operations.ChatMessageType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatRealtimeMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID messageId;
    private UUID conversationId;
    private UUID senderAccountId;
    private ChatMessageType messageType;
    private String content;
    private UUID replyToMessageId;
    private String relatedSchema;
    private String relatedTable;
    private UUID relatedId;
    private boolean deleted;
    private String deletedAt;
    private String editedAt;
    private String createdAt;
    private List<ChatRealtimeAttachment> attachments = new ArrayList<>();
}
