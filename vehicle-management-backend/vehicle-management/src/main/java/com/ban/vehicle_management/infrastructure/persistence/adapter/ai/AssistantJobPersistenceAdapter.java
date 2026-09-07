package com.ban.vehicle_management.infrastructure.persistence.adapter.ai;

import com.ban.vehicle_management.application.ai.port.out.AssistantJobPortOut;
import com.ban.vehicle_management.domain.ai.model.AssistantJob;
import com.ban.vehicle_management.infrastructure.mapper.ai.AssistantJobPersistenceMapper;
import com.ban.vehicle_management.infrastructure.persistence.database.repository.ai.AssistantJobRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AssistantJobPersistenceAdapter implements AssistantJobPortOut {

    private final AssistantJobRepository repository;
    private final AssistantJobPersistenceMapper mapper;

    public AssistantJobPersistenceAdapter(AssistantJobRepository repository, AssistantJobPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Optional<AssistantJob> findByInputMessageId(UUID inputMessageId) {
        return repository.findByInputMessageId(inputMessageId).map(mapper::toDomain);
    }

    @Override
    public AssistantJob save(AssistantJob job) {
        return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(job)));
    }

    @Override
    @Transactional
    public List<AssistantJob> claimDueJobs(Instant now, Instant lockExpiresAt, String lockedBy, int limit) {
        return repository.claimDueJobs(now, lockExpiresAt, lockedBy, Math.max(1, limit))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
