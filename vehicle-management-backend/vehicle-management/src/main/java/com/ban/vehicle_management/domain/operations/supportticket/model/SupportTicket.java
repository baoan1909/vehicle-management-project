package com.ban.vehicle_management.domain.operations.supportticket.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
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
    private UUID customerId;
    private UUID categoryId;
    private String categoryCode;
    private String categoryName;
    private SupportTicketCategoryPriority priority;
    private String title;
    private String content;
    private SupportTicketStatus status;
    private UUID assignedTo;
    private Instant resolvedAt;
    private String resolutionNote;
    private Instant closedAt;
    private UUID closedBy;
    private Integer reopenCount;
    private Instant lastReopenedAt;
    private SupportTicketSource source;
    private UUID sourceConversationId;
    private UUID sourceMessageId;
    private String idempotencyKey;
    private Instant firstRespondedAt;
}
