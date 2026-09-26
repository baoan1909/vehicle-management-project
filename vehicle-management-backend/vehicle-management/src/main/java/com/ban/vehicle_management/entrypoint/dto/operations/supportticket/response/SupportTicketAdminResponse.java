package com.ban.vehicle_management.entrypoint.dto.operations.supportticket.response;

import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketSource;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SupportTicketAdminResponse {
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
    private java.time.Instant resolvedAt;
    private String resolutionNote;
    private java.time.Instant closedAt;
    private UUID closedBy;
    private Integer reopenCount;
    private java.time.Instant lastReopenedAt;
    private java.time.Instant createdAt;
    private UUID createdBy;
    private java.time.Instant updatedAt;
    private UUID updatedBy;
    private SupportTicketSource source;
    private UUID sourceConversationId;
    private UUID sourceMessageId;
    private java.time.Instant firstRespondedAt;
}
