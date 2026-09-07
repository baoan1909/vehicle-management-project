package com.ban.vehicle_management.infrastructure.persistence.adapter.ai;

import com.ban.vehicle_management.application.ai.port.out.AiRunPortOut;
import com.ban.vehicle_management.domain.ai.model.AiRun;
import com.ban.vehicle_management.infrastructure.mapper.ai.AiRunPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.AiRunRepository;
import com.ban.vehicle_management.shared.enumeration.ai.AiRunStatus;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AiRunPersistenceAdapter implements AiRunPortOut {

    private final AiRunRepository repository;
    private final AiRunPersistenceMapper mapper;

    public AiRunPersistenceAdapter(AiRunRepository repository, AiRunPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public AiRun save(AiRun run) {
        return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(run)));
    }

    @Override
    public boolean existsSuccessfulRunForInputMessage(UUID inputMessageId) {
        return repository.existsByInputMessageIdAndStatus(inputMessageId, AiRunStatus.SUCCEEDED);
    }
}
