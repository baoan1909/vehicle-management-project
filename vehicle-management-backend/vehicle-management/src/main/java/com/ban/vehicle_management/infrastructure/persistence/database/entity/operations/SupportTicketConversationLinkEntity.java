package com.ban.vehicle_management.infrastructure.persistence.database.entity.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketConversationLinkReason;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketConversationLinkStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "support_ticket_conversation_links", schema = "operations")
@Getter
@Setter
public class SupportTicketConversationLinkEntity extends AuditableEntity {
    @Id
    @Column(name = "support_ticket_conversation_link_id", nullable = false)
    private UUID supportTicketConversationLinkId;

    @Column(name = "support_ticket_id", nullable = false)
    private UUID supportTicketId;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SupportTicketConversationLinkStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_reason", nullable = false)
    private SupportTicketConversationLinkReason linkReason;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    @Column(name = "linked_by_account_id")
    private UUID linkedByAccountId;

    @Column(name = "unlinked_at")
    private Instant unlinkedAt;
}
