package com.ban.vehicle_management.entrypoint.dto.operations.supportticket.response;

import com.ban.vehicle_management.entrypoint.dto.operations.chatconversation.response.ChatConversationUserResponse;

public record SupportTicketChatIntakeResponse(
        SupportTicketUserResponse ticket,
        ChatConversationUserResponse conversation,
        boolean reusedActiveTicket
) {
}
