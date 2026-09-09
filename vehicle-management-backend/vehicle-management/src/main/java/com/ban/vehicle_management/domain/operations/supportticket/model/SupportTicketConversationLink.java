package com.ban.vehicle_management.domain.operations.supportticket.model;

import com.ban.vehicle_management.domain.common.model.AuditableDomainModel;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketConversationLinkReason;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketConversationLinkStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupportTicketConversationLink extends AuditableDomainModel {
    private UUID supportTicketConversationLinkId;
    private UUID supportTicketId;
    private UUID conversationId;
    private SupportTicketConversationLinkStatus status;
    private SupportTicketConversationLinkReason linkReason;
    private Instant linkedAt;
    private UUID linkedByAccountId;
    private Instant unlinkedAt;
}
