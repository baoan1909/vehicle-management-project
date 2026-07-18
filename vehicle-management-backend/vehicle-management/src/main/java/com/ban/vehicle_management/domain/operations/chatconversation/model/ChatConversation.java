package com.ban.vehicle_management.domain.operations.chatconversation.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationStatus;
import com.ban.vehicle_management.shared.enumeration.operations.ChatConversationType;
import java.time.Instant;
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
public class ChatConversation extends AuditableDomainModel {

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
    private Instant lastMessageAt;
    private String metadata;
    private List<ChatConversationParticipant> participants = List.of();
}
