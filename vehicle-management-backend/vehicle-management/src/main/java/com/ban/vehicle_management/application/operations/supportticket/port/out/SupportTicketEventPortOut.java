package com.ban.vehicle_management.application.operations.supportticket.port.out;

import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicketEvent;
import java.util.Optional;
import java.util.UUID;

public interface SupportTicketEventPortOut {
    SupportTicketEvent save(SupportTicketEvent event);
    Optional<SupportTicketEvent> findByConversationIdAndIdempotencyKey(UUID conversationId, String idempotencyKey);
}
