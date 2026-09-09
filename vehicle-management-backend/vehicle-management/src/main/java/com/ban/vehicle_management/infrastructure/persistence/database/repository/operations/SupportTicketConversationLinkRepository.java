package com.ban.vehicle_management.infrastructure.persistence.database.repository.operations;

import com.ban.vehicle_management.infrastructure.persistence.database.entity.operations.SupportTicketConversationLinkEntity;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketConversationLinkStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportTicketConversationLinkRepository extends JpaRepository<SupportTicketConversationLinkEntity, UUID> {
    boolean existsBySupportTicketId(UUID supportTicketId);

    Optional<SupportTicketConversationLinkEntity> findFirstBySupportTicketIdAndStatus(
            UUID supportTicketId,
            SupportTicketConversationLinkStatus status
    );

    Optional<SupportTicketConversationLinkEntity> findFirstBySupportTicketIdOrderByLinkedAtDesc(UUID supportTicketId);
}
