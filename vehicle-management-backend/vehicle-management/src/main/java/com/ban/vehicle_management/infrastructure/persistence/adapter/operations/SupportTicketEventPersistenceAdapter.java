package com.ban.vehicle_management.infrastructure.persistence.adapter.operations;

import com.ban.vehicle_management.application.operations.supportticket.port.out.SupportTicketEventPortOut;
import com.ban.vehicle_management.domain.operations.supportticket.model.SupportTicketEvent;
import com.ban.vehicle_management.infrastructure.mapper.operations.SupportTicketEventPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.operations.SupportTicketEventRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SupportTicketEventPersistenceAdapter implements SupportTicketEventPortOut {

    private final SupportTicketEventRepository repository;
    private final SupportTicketEventPersistenceMapper mapper;

    public SupportTicketEventPersistenceAdapter(
            SupportTicketEventRepository repository,
            SupportTicketEventPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public SupportTicketEvent save(SupportTicketEvent event) {
        return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(event)));
    }

    @Override
    public Optional<SupportTicketEvent> findByConversationIdAndIdempotencyKey(
            UUID conversationId,
            String idempotencyKey
    ) {
        return repository.findByConversationIdAndIdempotencyKey(conversationId, idempotencyKey)
                .map(mapper::toDomain);
    }
}
