package com.ban.vehicle_management.infrastructure.persistence.database.entity.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.common.AuditableEntity;
import com.ban.vehicle_management.infrastructure.persistence.database.entity.iam.AccountEntity;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCloseReason;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketSource;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Formula;

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

    @Column(name = "requester_account_id", nullable = false)
    private UUID requesterAccountId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_account_id", referencedColumnName = "account_id", insertable = false, updatable = false)
    private AccountEntity requesterAccount;

    @Column(name = "requester_role_code", nullable = false, length = 50)
    private String requesterRoleCode;

    @Column(name = "requester_name_snapshot", length = 150)
    private String requesterNameSnapshot;

    @Column(name = "requester_email_snapshot", length = 255)
    private String requesterEmailSnapshot;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "parking_lot_id")
    private UUID parkingLotId;

    @Column(name = "feature_code", length = 100)
    private String featureCode;

    @Column(name = "context_key", length = 255)
    private String contextKey;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", referencedColumnName = "category_id", insertable = false, updatable = false)
    private SupportTicketCategoryEntity category;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SupportTicketStatus status;

    @Column(name = "assigned_to")
    private UUID assignedTo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to", referencedColumnName = "account_id", insertable = false, updatable = false)
    private AccountEntity assignedToAccount;

    @Formula("(select account.username from iam.accounts account where account.account_id = assigned_to)")
    private String assignedToUsername;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolution_note")
    private String resolutionNote;

    @Column(name = "last_context_activity_at")
    private Instant lastContextActivityAt;

    @Column(name = "auto_close_at")
    private Instant autoCloseAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "closed_by")
    private UUID closedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by", referencedColumnName = "account_id", insertable = false, updatable = false)
    private AccountEntity closedByAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "close_reason", length = 30)
    private SupportTicketCloseReason closeReason;

    @Column(name = "reopen_count", nullable = false)
    private Integer reopenCount;

    @Column(name = "last_reopened_at")
    private Instant lastReopenedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    private SupportTicketSource source;

    @Column(name = "source_conversation_id")
    private UUID sourceConversationId;

    @Column(name = "source_message_id")
    private UUID sourceMessageId;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "first_responded_at")
    private Instant firstRespondedAt;
}
