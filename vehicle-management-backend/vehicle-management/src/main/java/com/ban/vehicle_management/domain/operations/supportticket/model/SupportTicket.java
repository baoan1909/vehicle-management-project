package com.ban.vehicle_management.domain.operations.supportticket.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCloseReason;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketSource;
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
public class SupportTicket extends AuditableDomainModel {
    private UUID supportTicketId;
    private UUID requesterAccountId;
    private String requesterRoleCode;
    private String requesterNameSnapshot;
    private String requesterEmailSnapshot;
    private UUID organizationId;
    private UUID parkingLotId;
    private String featureCode;
    private String contextKey;
    private UUID categoryId;
    private String categoryCode;
    private String categoryName;
    private SupportTicketCategoryPriority priority;
    private String title;
    private String content;
    private SupportTicketStatus status;
    private UUID assignedTo;
    private String assignedToUsername;
    private Instant resolvedAt;
    private String resolutionNote;
    private Instant lastContextActivityAt;
    private Instant autoCloseAt;
    private Instant closedAt;
    private UUID closedBy;
    private SupportTicketCloseReason closeReason;
    private Integer reopenCount;
    private Instant lastReopenedAt;
    private SupportTicketSource source;
    private UUID sourceConversationId;
    private UUID sourceMessageId;
    private String idempotencyKey;
    private Instant firstRespondedAt;
}
