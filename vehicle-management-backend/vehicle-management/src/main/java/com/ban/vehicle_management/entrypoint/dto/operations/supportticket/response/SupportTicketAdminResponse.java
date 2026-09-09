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
    private String resolvedAt;
    private String resolutionNote;
    private String closedAt;
    private UUID closedBy;
    private Integer reopenCount;
    private String lastReopenedAt;
    private String createdAt;
    private UUID createdBy;
    private String updatedAt;
    private UUID updatedBy;
    private SupportTicketSource source;
    private UUID sourceConversationId;
    private UUID sourceMessageId;
    private String firstRespondedAt;
}
