package com.ban.vehicle_management.domain.operations.supportticket.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.SupportTicketPriority;
import com.ban.vehicle_management.shared.enumeration.SupportTicketStatus;
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
    private String title;
    private String content;
    private SupportTicketStatus status;
    private SupportTicketPriority priority;
    private UUID assignedTo;
    private Instant resolvedAt;
}

