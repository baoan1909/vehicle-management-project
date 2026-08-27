package com.ban.vehicle_management.application.operations.supportticket.port.in;

import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;
import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketCategoryPriority;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketStatus;
import java.util.List;
import java.util.UUID;

public interface SupportTicketPortIn {
    SupportTicket createTicket(SupportTicket supportTicket);
    SupportTicket createTicketFromConversation(SupportTicket supportTicket, UUID conversationId);
    SupportTicket getTicketById(UUID supportTicketId);

    List<SupportTicket> getTickets(
            UUID customerId,
            UUID categoryId,
            UUID assignedTo,
            SupportTicketStatus status,
            SupportTicketCategoryPriority priority,
            String keyword
    );

    SupportTicket updateTicket(UUID supportTicketId, SupportTicket supportTicket);
    SupportTicket assignTicket(UUID supportTicketId, UUID assignedTo);
    SupportTicket startProgress(UUID supportTicketId);
    SupportTicket resolveTicket(UUID supportTicketId, String resolutionNote);
    SupportTicket reopenTicket(UUID supportTicketId);
    SupportTicket closeTicket(UUID supportTicketId);
    ChatConversation openCustomerConversationForReply(UUID supportTicketId);
}
