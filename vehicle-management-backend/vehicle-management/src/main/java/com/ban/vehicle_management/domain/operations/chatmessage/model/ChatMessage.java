package com.ban.vehicle_management.domain.operations.chatmessage.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.operations.ChatMessageType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage extends AuditableDomainModel {

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
    private String metadata;
    private boolean deleted;
    private Instant deletedAt;
    private Instant editedAt;
    private List<ChatMessageAttachment> attachments = new ArrayList<>();
}
