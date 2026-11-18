package com.ban.vehicle_management.domain.operations.supportticket.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketEventType;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketEvent extends AuditableDomainModel {
    private UUID eventId;
    private UUID supportTicketId;
    private UUID conversationId;
    private UUID actorAccountId;
    private SupportTicketEventType eventType;
    private SupportTicketStatus fromStatus;
    private SupportTicketStatus toStatus;
    private SupportTicketStatus snapshotStatus;
    private String snapshotPayload;
    private String idempotencyKey;
}
