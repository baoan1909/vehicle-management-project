package com.ban.vehicle_management.entrypoint.dto.operations.approvalrequest.response;

import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketEscalationDecision;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketEscalationReason;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SupportTicketEscalationResponse {
    private UUID escalationId;
    private UUID supportTicketId;
    private ApprovalRequestStatus status;
    private SupportTicketEscalationReason reasonCode;
    private String description;
    private UUID requestedBy;
    private UUID currentAssigneeId;
    private String requestedAt;
    private UUID reviewedBy;
    private String reviewedAt;
    private SupportTicketEscalationDecision decision;
    private UUID reassignedTo;
    private String decisionNote;
}
