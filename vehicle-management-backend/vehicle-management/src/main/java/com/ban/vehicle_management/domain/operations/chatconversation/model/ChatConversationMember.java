package com.ban.vehicle_management.domain.operations.chatconversation.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.operations.ChatMemberRole;
import com.ban.vehicle_management.shared.enumeration.operations.ChatMemberStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatConversationMember extends AuditableDomainModel {

    private UUID conversationMemberId;
    private UUID conversationId;
    private UUID accountId;
    private ChatMemberRole memberRole;
    private ChatMemberStatus status;
    private UUID lastReadMessageId;
    private Instant mutedUntil;
    private Instant joinedAt;
    private Instant leftAt;
}
