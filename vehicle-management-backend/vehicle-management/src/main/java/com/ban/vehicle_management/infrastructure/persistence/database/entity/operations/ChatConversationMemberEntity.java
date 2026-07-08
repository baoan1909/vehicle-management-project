package com.ban.vehicle_management.infrastructure.persistence.database.entity.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.operations.ChatMemberRole;
import com.ban.vehicle_management.shared.enumeration.operations.ChatMemberStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chat_conversation_members", schema = "operations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatConversationMemberEntity extends AuditableEntity {

    @Id
    @Column(name = "conversation_member_id", nullable = false)
    private UUID conversationMemberId;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "member_role", nullable = false)
    private ChatMemberRole memberRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ChatMemberStatus status;

    @Column(name = "last_read_message_id")
    private UUID lastReadMessageId;

    @Column(name = "muted_until")
    private Instant mutedUntil;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;
}
