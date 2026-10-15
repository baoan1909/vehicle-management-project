package com.ban.vehicle_management.entrypoint.dto.operations.supportticket.response;

import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Customer-facing support ticket view. Internal audit fields and conversation
 * linkage internals are omitted; ownership is derived from the authenticated
 * account via the {@code /mine} endpoints.
 */
@Getter
@Setter
@NoArgsConstructor
public class SupportTicketUserResponse {

    private UUID supportTicketId;
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
    private Instant createdAt;
    private Instant updatedAt;
}
