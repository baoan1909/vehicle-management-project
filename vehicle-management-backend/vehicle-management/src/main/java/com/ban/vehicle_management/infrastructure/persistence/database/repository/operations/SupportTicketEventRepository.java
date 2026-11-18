package com.ban.vehicle_management.infrastructure.persistence.database.repository.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.SupportTicketEventEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportTicketEventRepository extends JpaRepository<SupportTicketEventEntity, UUID> {
    Optional<SupportTicketEventEntity> findByConversationIdAndIdempotencyKey(UUID conversationId, String idempotencyKey);
}
