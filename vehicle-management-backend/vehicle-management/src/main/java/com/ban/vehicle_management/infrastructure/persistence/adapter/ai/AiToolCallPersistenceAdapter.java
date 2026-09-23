package com.ban.vehicle_management.infrastructure.persistence.adapter.ai;

import com.ban.vehicle_management.application.ai.port.out.AiToolCallPortOut;
import com.ban.vehicle_management.domain.ai.model.AiToolCall;
import com.ban.vehicle_management.infrastructure.mapper.ai.AiToolCallPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.AiToolCallRepository;
import com.ban.vehicle_management.shared.enumeration.ai.AiToolCallStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AiToolCallPersistenceAdapter implements AiToolCallPortOut {

    private final AiToolCallRepository repository;
    private final AiToolCallPersistenceMapper mapper;

    public AiToolCallPersistenceAdapter(AiToolCallRepository repository, AiToolCallPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public AiToolCall save(AiToolCall toolCall) {
        return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(toolCall)));
    }

    @Override
    public Optional<AiToolCall> findById(UUID toolCallId) {
        return repository.findById(toolCallId).map(mapper::toDomain);
    }

    @Override
    public Optional<AiToolCall> findByIdForUpdate(UUID toolCallId) {
        return repository.findByIdForUpdate(toolCallId).map(mapper::toDomain);
    }

    @Override
    public List<AiToolCall> findByInputMessageId(UUID inputMessageId) {
        return repository.findByInputMessageIdOrderByCreatedAtDesc(inputMessageId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<AiToolCall> findExpired(AiToolCallStatus status, Instant now) {
        return repository.findByStatusAndExpiresAtBefore(status, now).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
