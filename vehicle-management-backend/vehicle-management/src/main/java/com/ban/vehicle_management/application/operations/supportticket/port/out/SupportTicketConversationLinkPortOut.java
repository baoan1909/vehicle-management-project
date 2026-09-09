package com.ban.vehicle_management.application.operations.supportticket.port.out;

import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicketConversationLink;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketConversationLinkReason;
import java.util.Optional;
import java.util.UUID;

public interface SupportTicketConversationLinkPortOut {
    Optional<SupportTicketConversationLink> findActiveBySupportTicketId(UUID supportTicketId);
    Optional<SupportTicketConversationLink> findMostRecentBySupportTicketId(UUID supportTicketId);
    boolean existsBySupportTicketId(UUID supportTicketId);
    SupportTicketConversationLink activate(
            UUID supportTicketId,
            UUID conversationId,
            SupportTicketConversationLinkReason reason,
            UUID linkedByAccountId
    );
    void deactivate(UUID supportTicketId);
}
