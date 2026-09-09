package com.ban.vehicle_management.application.operations.approvalrequest.port.in;

import com.ban.vehicle_management.application.operations.approvalrequest.model.command.CreateSupportTicketEscalationCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.command.ReviewSupportTicketEscalationCommand;
import com.ban.vehicle_management.application.operations.approvalrequest.model.result.SupportTicketEscalationResult;
import com.ban.vehicle_management.shared.enumeration.operations.ApprovalRequestStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SupportTicketEscalationPortIn {

    SupportTicketEscalationResult create(UUID supportTicketId, CreateSupportTicketEscalationCommand command);

    Optional<SupportTicketEscalationResult> getMyCurrent(UUID supportTicketId);

    List<SupportTicketEscalationResult> getAll(ApprovalRequestStatus status);

    SupportTicketEscalationResult approve(UUID escalationId, ReviewSupportTicketEscalationCommand command);

    SupportTicketEscalationResult reject(UUID escalationId, String note);
}
