package com.ban.vehicle_management.infrastructure.persistence.database.entity.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketEventType;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "support_ticket_events", schema = "operations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketEventEntity extends AuditableEntity {
    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "support_ticket_id", nullable = false)
    private UUID supportTicketId;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "actor_account_id")
    private UUID actorAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private SupportTicketEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private SupportTicketStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", length = 20)
    private SupportTicketStatus toStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "snapshot_status", nullable = false, length = 20)
    private SupportTicketStatus snapshotStatus;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "snapshot_payload", nullable = false, columnDefinition = "jsonb")
    private String snapshotPayload;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;
}
