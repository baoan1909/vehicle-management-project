package com.ban.vehicle_management.infrastructure.persistence.adapter.operations;

import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketConversationLinkPortOut;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicketConversationLink;
import com.ban.vehicle_management.infrastructure.mapper.operations.SupportTicketConversationLinkPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.SupportTicketConversationLinkRepository;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketConversationLinkReason;
import com.ban.vehicle_management.shared.enumeration.operations.SupportTicketConversationLinkStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SupportTicketConversationLinkPersistenceAdapter implements SupportTicketConversationLinkPortOut {
    private final SupportTicketConversationLinkRepository repository;
    private final SupportTicketConversationLinkPersistenceMapper mapper;

    public SupportTicketConversationLinkPersistenceAdapter(
            SupportTicketConversationLinkRepository repository,
            SupportTicketConversationLinkPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<SupportTicketConversationLink> findActiveBySupportTicketId(UUID supportTicketId) {
        return repository.findFirstBySupportTicketIdAndStatus(supportTicketId, SupportTicketConversationLinkStatus.ACTIVE)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<SupportTicketConversationLink> findMostRecentBySupportTicketId(UUID supportTicketId) {
        return repository.findFirstBySupportTicketIdOrderByLinkedAtDesc(supportTicketId)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsBySupportTicketId(UUID supportTicketId) {
        return repository.existsBySupportTicketId(supportTicketId);
    }

    @Override
    public SupportTicketConversationLink activate(
            UUID supportTicketId,
            UUID conversationId,
            SupportTicketConversationLinkReason reason,
            UUID linkedByAccountId
    ) {
        SupportTicketConversationLink activeLink = findActiveBySupportTicketId(supportTicketId).orElse(null);
        if (activeLink != null && activeLink.getConversationId().equals(conversationId)) {
            return activeLink;
        }

        Instant now = Instant.now();
        if (activeLink != null) {
            activeLink.setStatus(SupportTicketConversationLinkStatus.HISTORICAL);
            activeLink.setUnlinkedAt(now);
            repository.saveAndFlush(mapper.toEntity(activeLink));
        }

        SupportTicketConversationLink link = new SupportTicketConversationLink();
        link.setSupportTicketConversationLinkId(UUID.randomUUID());
        link.setSupportTicketId(supportTicketId);
        link.setConversationId(conversationId);
        link.setStatus(SupportTicketConversationLinkStatus.ACTIVE);
        link.setLinkReason(reason);
        link.setLinkedAt(now);
        link.setLinkedByAccountId(linkedByAccountId);
        return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(link)));
    }

    @Override
    public void deactivate(UUID supportTicketId) {
        SupportTicketConversationLink activeLink = findActiveBySupportTicketId(supportTicketId).orElse(null);
        if (activeLink == null) {
            return;
        }
        activeLink.setStatus(SupportTicketConversationLinkStatus.HISTORICAL);
        activeLink.setUnlinkedAt(Instant.now());
        repository.saveAndFlush(mapper.toEntity(activeLink));
    }
}
