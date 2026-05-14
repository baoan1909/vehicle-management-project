package com.ban.vehicle_management.infrastructure.persistence.operations.supportticket;

import com.ban.vehicle_management.infrastructure.persistence.common.entity.AuditableEntity;
import com.ban.vehicle_management.shared.enumeration.SupportTicketPriority;
import com.ban.vehicle_management.shared.enumeration.SupportTicketStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "support_tickets", schema = "operations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketEntity extends AuditableEntity {

    @Id
    @Column(name = "support_ticket_id", nullable = false)
    private UUID supportTicketId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SupportTicketStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private SupportTicketPriority priority;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

}
