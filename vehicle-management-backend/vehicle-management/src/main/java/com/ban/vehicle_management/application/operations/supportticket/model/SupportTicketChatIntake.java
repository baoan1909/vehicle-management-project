package com.ban.vehicle_management.application.operations.supportticket.model;

import com.ban.vehicle_management.domain.operations.chatconversation.model.ChatConversation;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicket;

/**
 * Result of opening the authenticated-customer support intake. The ticket remains the
 * workflow source of truth; the conversation is its communication channel.
 */
public record SupportTicketChatIntake(
        SupportTicket ticket,
        ChatConversation conversation,
        boolean reusedActiveTicket
) {
}
